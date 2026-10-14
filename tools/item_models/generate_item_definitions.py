"""
Generates the 1.21.4+ item model definitions (assets/cobblemon/items/<id>.json) for every Cobblemon item and moves the
pre-1.21.4 model "overrides" of assets/cobblemon/models/item into them.

Item ids are read from CobblemonItems.kt. Run from the repository root:

    python tools/item_models/generate_item_definitions.py

Re-running is safe: definitions are regenerated, and overrides already removed from the item models are read from
git (the base commit's model files) when --overrides-from is given, otherwise from the current files.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
COMMON = ROOT / "src/main/kotlin/com/cobblemon/mod/common"
ASSETS = ROOT / "src/main/resources/assets/cobblemon"
MODELS = ASSETS / "models/item"
ITEMS = ASSETS / "items"

# Registration helpers of CobblemonItems whose first argument is the item id, and the suffix they append to it.
NAMED_HELPERS = {
    "create": "", "blockItem": "", "itemNameBlockItem": "", "noSettingsItem": "", "itemWithRarity": "",
    "mulchItem": "", "candyItem": "", "wearableItem": "", "wearableBlockItem": "", "regionalFoodItem": "",
    "heldItem": "", "hyperTrainingItem": "", "compostableItem": "", "compostableHeldItem": "",
    "compostableBlockItem": "", "compostableItemNameBlockItem": "", "glowItem": "", "mintItem": "",
    "berryItem": "_berry", "apricornItem": "_apricorn", "apricornSeedItem": "_apricorn_seed",
    "mintSeed": "_mint_seeds", "mintLeaf": "_mint_leaf",
}

# Tint sources registered in CobblemonClient.registerItemColors, and the tint indices each provider colours.
TINTS = {
    "aprijuice": ("cobblemon:aprijuice", 2),
    "poke_snack": ("cobblemon:poke_snack", 1),
    "poke_cake": ("cobblemon:poke_snack", 1),
    "poke_bait": ("cobblemon:poke_bait", 3),
    "ponigiri": ("cobblemon:ponigiri", 1),
    "sinister_tea": ("cobblemon:sinister_tea", 2),
}

# Range select properties registered in CobblemonModelPredicateRegistry. Old predicates without a namespace were
# vanilla ones.
PROPERTIES = {
    "cast": "cobblemon:cast",
    "custom_model_data": "minecraft:custom_model_data",
    "minecraft:custom_model_data": "minecraft:custom_model_data",
    "cobblemon:ponigiri_overlay": "cobblemon:ponigiri_overlay",
    "cobblemon:aprijuice_quality": "cobblemon:aprijuice_quality",
}


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def parse_items():
    """The ids of the items CobblemonItems registers, with the block of block items when it is given directly."""
    text = strip_comments((COMMON / "CobblemonItems.kt").read_text(encoding="utf-8"))
    # Only property initialisers register items; helper bodies (from the first private fun on) are left out.
    body = text[: text.index("private fun blockItem(")]
    statements = re.split(r"\n\s*(?=(?:@JvmField\s+)?(?:val|var|fun|private|init)\b)", body)
    items = {}
    for statement in statements:
        m = re.match(r"\s*(?:@JvmField\s+)?(?:val|var)\s+([A-Z0-9_]+)\s*(?::[^=]+)?=\s*(.*)", statement, re.S)
        if not m:
            continue
        # Stats ids are their lowercase names (x_${Stats.ATTACK.identifier.path} is x_attack).
        rhs = re.sub(r"\$\{Stats\.([A-Z_]+)\.identifier\.path\}", lambda s: s.group(1).lower(), m.group(2))
        call = re.match(r"(?:this\.)?(\w+)\s*\((.*)", rhs, re.S)
        if not call:
            continue
        helper, args = call.groups()
        block = re.search(r"CobblemonBlocks\.([A-Z0-9_]+)", args)
        if helper in NAMED_HELPERS:
            name = re.match(r'\s*(?:name\s*=\s*)?"([a-z0-9_]+)"', args)
            if not name:
                sys.exit(f"Unparsed {helper} call for {m.group(1)}")
            item_id = name.group(1) + NAMED_HELPERS[helper]
        elif helper == "pokeBallItem":
            item_id = re.match(r"\s*PokeBalls\.([A-Z0-9_]+)", args).group(1).lower()
        elif helper == "pokerodItem":
            item_id = re.match(r'\s*cobblemonResource\("([a-z0-9_]+)"\)', args).group(1)
        elif helper == "pokedexItem":
            item_id = "pokedex_" + re.match(r"\s*PokedexType\.([A-Z]+)", args).group(1).lower()
        elif helper == "campfirePotItem":
            item_id = "campfire_pot_" + re.search(r"CampfirePotColor\.([A-Z]+)", args).group(1).lower()
        elif helper == "aprijuiceItem":
            item_id = "aprijuice_" + re.match(r"\s*Apricorn\.([A-Z]+)", args).group(1).lower()
        else:
            continue
        if item_id in items:
            sys.exit(f"Duplicate item id {item_id}")
        items[item_id] = {"const": m.group(1), "helper": helper, "block": block.group(1) if block else None}
    return items


def block_ids():
    """CobblemonBlocks constant -> block id."""
    text = strip_comments((COMMON / "CobblemonBlocks.kt").read_text(encoding="utf-8"))
    ids = {}
    for m in re.finditer(r"val\s+([A-Z0-9_]+)\s*(?::[^=]+)?=\s*(?:this\.)?\w+\(\s*(?:name\s*=\s*)?\"([a-z0-9_]+)\"", text):
        ids[m.group(1)] = m.group(2)
    return ids


def model(ref, tints=None):
    node = {"type": "minecraft:model", "model": ref}
    if tints:
        node["tints"] = tints
    return node


def tint_list(item_id):
    key = "aprijuice" if item_id.startswith("aprijuice_") else item_id
    if key not in TINTS:
        return None
    source, count = TINTS[key]
    return [{"type": source, "tint_index": i} for i in range(count)]


def model_exists(ref):
    namespace, path = ref.split(":")
    return (ROOT / f"src/main/resources/assets/{namespace}/models/{path}.json").exists()


def overrides_to_node(item_id, base_ref, overrides, tints):
    """An item model that picks among a model's former overrides."""
    keys = {k for o in overrides for k in o["predicate"]}
    if len(keys) != 1:
        sys.exit(f"{item_id}: overrides use several predicates {keys}")
    (key,) = keys
    if key not in PROPERTIES:
        sys.exit(f"{item_id}: no item model property for predicate {key}")
    # Overrides applied the last one whose threshold was reached; range_dispatch applies the highest threshold
    # reached, so later entries win ties.
    entries = {}
    for o in overrides:
        entries[float(o["predicate"][key])] = o["model"]
    node = {"type": "minecraft:range_dispatch", "property": PROPERTIES[key]}
    if PROPERTIES[key] == "minecraft:custom_model_data":
        node["index"] = 0
    node["entries"] = [{"threshold": t, "model": model(m, tints)} for t, m in sorted(entries.items())]
    node["fallback"] = model(base_ref, tints)
    return node


def read_model(path, base_rev):
    if base_rev:
        rel = path.relative_to(ROOT).as_posix()
        out = subprocess.run(["git", "show", f"{base_rev}:{rel}"], cwd=ROOT, capture_output=True, text=True, encoding="utf-8")
        if out.returncode == 0:
            return json.loads(out.stdout)
    return json.loads(path.read_text(encoding="utf-8"))


def display_select(cases, fallback):
    return {
        "type": "minecraft:select",
        "property": "minecraft:display_context",
        "cases": [{"when": when, "model": node} for when, node in cases],
        "fallback": fallback,
    }


def item_node(item_id, info, blocks, base_rev):
    """The item model of one item, or None when it has no model to show."""
    tints = tint_list(item_id)
    ref = f"cobblemon:item/{item_id}"
    path = MODELS / f"{item_id}.json"
    if not path.exists():
        block = blocks.get(info["block"]) if info["block"] else None
        if block and model_exists(f"cobblemon:block/{block}"):
            return model(f"cobblemon:block/{block}", tints)
        return None

    data = read_model(path, base_rev)
    node = overrides_to_node(item_id, ref, data["overrides"], tints) if "overrides" in data else model(ref, tints)

    helper = info["helper"]
    if helper == "pokeBallItem":
        # The 3D model everywhere but in GUIs and item frames (ItemRendererMixin before 1.21.4).
        return display_select([(["gui", "fixed"], node)], model(f"cobblemon:item/{item_id}_model"))
    if helper == "pokedexItem":
        # Scanning while in use, "off" when not held (ItemRendererMixin before 1.21.4).
        held = {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_true": model(f"cobblemon:item/{item_id}_model_scanning"),
            "on_false": model(f"cobblemon:item/{item_id}_model"),
        }
        return display_select(
            [(["gui", "fixed"], node), (["ground", "head", "none"], model(f"cobblemon:item/{item_id}_model_off"))],
            held,
        )
    if helper in ("wearableItem", "wearableBlockItem"):
        worn = f"cobblemon:item/wearable/{item_id}"
        if model_exists(worn):
            return display_select([("head", model(worn))], node)
    return node


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--overrides-from", help="git revision to read item model overrides from")
    args = parser.parse_args()

    items = parse_items()
    blocks = block_ids()
    ITEMS.mkdir(exist_ok=True)

    missing = []
    for item_id, info in sorted(items.items()):
        node = item_node(item_id, info, blocks, args.overrides_from)
        if node is None:
            missing.append(item_id)
            continue
        write(ITEMS / f"{item_id}.json", {"model": node})

    # Models the code selects by id through the item_model component (LecternBlockEntityRenderer: Pokedexes on
    # lecterns), and the TM models, so the code can select those the same way.
    extra = [p.stem for p in MODELS.glob("pokedex_*_model*.json")]
    extra += [f"tm/{p.stem}" for p in (MODELS / "tm").glob("*_tm.json")]
    for ref in extra:
        target = ITEMS / f"{ref}.json"
        target.parent.mkdir(exist_ok=True)
        write(target, {"model": model(f"cobblemon:item/{ref}")})

    # Drop the overrides from the item models; they are ignored since 1.21.4.
    stripped = 0
    for path in MODELS.rglob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        if "overrides" in data:
            del data["overrides"]
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            stripped += 1

    print(f"{len(items)} items, {len(items) - len(missing)} definitions, {len(extra)} extra definitions, "
          f"{stripped} models stripped of overrides")
    if missing:
        print("No model for: " + ", ".join(missing))


def write(path, data):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
