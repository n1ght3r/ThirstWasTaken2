"""Turn the captures of tools/agent/shots/recipes.jsonl into the docs' recipe images.

For each capture: the crafting panel's top 71 GUI px (down to the bottom of the grid) and its last 5 for
the bottom edge, at the capture's own scale, with the result's tooltip drawn by
tools/generate_docs_images.py's tooltip() where vanilla would put it for a pointer on the result slot,
from the lines the "*Tooltip" answers gave. The background stays transparent. The smithing table keeps
its top 76 GUI px instead, down past the armour stand, with the "Inventory" label under its slots painted
over.

    python tools/agent/shots/recipe_images.py run/26.2.x/agent/client [name ...]

Names, such as iron-flask-recipe, limit it to those images.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
import generate_docs_images as docs  # noqa: E402

SHOTS = {
    "clay-bowl-recipe": "bowlTooltip",
    "waterskin-recipe": "skinTooltip",
    "copper-canteen-recipe": "canteenTooltip",
    "iron-flask-recipe": "flaskTooltip",
    "distiller-boiler-recipe": "boilerTooltip",
    "diamond-flask-recipe": "diamondTooltip",
    "netherite-flask-recipe": "netheriteTooltip",
}
SMITHING = {"netherite-flask-recipe"}
PANEL_W, PANEL_H = 176, 166
KEEP_BOTTOM = 5
PANEL_GREY = (198, 198, 198, 255)
# Per screen: the rows kept from the top, the result slot, and a box of the kept rows to paint over.
CRAFTING = {"keep_top": 71, "result": (124, 35), "blank": None}
SMITHING_TABLE = {"keep_top": 76, "result": (98, 48), "blank": (7, 68, 70, 76)}
OUT = ROOT / "docs" / "public" / "screenshots" / "recipes"


def rgb(colour):
    if not colour:
        return (255, 255, 255)
    colour = colour.lstrip("#")
    return tuple(int(colour[i:i + 2], 16) for i in (0, 2, 4))


def answers(queue):
    found = {}
    for line in (queue / "out.jsonl").read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        reply = json.loads(line)
        found[reply.get("id")] = reply
    return found


def main():
    queue = Path(sys.argv[1])
    replies = answers(queue)
    font = docs.Font()
    only = set(sys.argv[2:])
    for name, tooltip_id in SHOTS.items():
        if only and name not in only:
            continue
        frame = Image.open(queue / "screenshots" / f"{name}.png").convert("RGBA")
        reply = next(r for r in replies.values() if r.get("command") == "client.capture"
                     and Path(r.get("result", {}).get("file", "")).stem == name)
        scale = int(round(reply["result"]["guiScale"]))
        gui_w = math.ceil(frame.width / scale)
        gui_h = math.ceil(frame.height / scale)
        left = (gui_w - PANEL_W) // 2
        top = (gui_h - PANEL_H) // 2

        layout = SMITHING_TABLE if name in SMITHING else CRAFTING
        keep_top = layout["keep_top"]
        # Where vanilla starts a tooltip's text for a pointer on the result slot's centre.
        text_x, text_y = layout["result"][0] + 8 + 12, layout["result"][1] + 8 - 12
        panel = frame.crop((left * scale, top * scale, (left + PANEL_W) * scale, (top + PANEL_H) * scale))
        lines = [(text, rgb(colour)) for text, colour in
                 zip(replies[tooltip_id]["result"]["lines"], replies[tooltip_id]["result"]["colours"])
                 if not text.startswith("§9")]
        width = max(font.width(text) for text, _ in lines)
        gui_out_w = max(PANEL_W, text_x + width + 5)
        gui_out_h = keep_top + KEEP_BOTTOM
        out = Image.new("RGBA", (gui_out_w * scale, gui_out_h * scale), (0, 0, 0, 0))
        out.alpha_composite(panel.crop((0, 0, PANEL_W * scale, keep_top * scale)), (0, 0))
        out.alpha_composite(panel.crop((0, (PANEL_H - KEEP_BOTTOM) * scale, PANEL_W * scale, PANEL_H * scale)),
                            (0, keep_top * scale))
        if layout["blank"]:
            x0, y0, x1, y1 = layout["blank"]
            out.paste(PANEL_GREY, (x0 * scale, y0 * scale, x1 * scale, y1 * scale))

        layer = Image.new("RGBA", (gui_out_w, gui_out_h), (0, 0, 0, 0))
        docs.tooltip(layer, font, text_x, text_y, lines)
        out.alpha_composite(layer.resize(out.size, Image.NEAREST))
        out.save(OUT / f"{name}.png", optimize=True)
        print(name, out.size, [text for text, _ in lines])


if __name__ == "__main__":
    main()
