# tools/agent — agent client scripts

Request files for the agent queue: one JSON request per line, `//` lines are comments, and `expect` /
`checks` beside a request are what `drive.py --verify` asserts afterwards. The protocol, the queue
directories and every command are in
[src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md](../../src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md).

```bash
./gradlew ":26.3.x:runClient" -Pagent=tools/agent/gameplay/parched.jsonl -Pquickplay=ParchedAgent
```

```bash
python tools/agent/drive.py run/26.3.x/agent/client tools/agent/gameplay/parched.jsonl --verify
```

Every path is relative to the repository root. Each script's header says which nodes it runs on, the
world it needs and how to read the result; read it before running one.

## Layout

| Folder | What is in it |
|---|---|
| `drive.py` | Sends a file or standard input to a queue, waits for the answers, and `--verify`s a finished run |
| `make_gif.py` | Turns a `client.record` folder into a GIF or WebP, drawing the virtual pointer as a cursor. Options in its docstring |
| `new_world.py`, `nbt.py` | Makes the throwaway world a script runs in. See [A world for a script](#a-world-for-a-script) |
| `smoke/` | Checks with no gameplay: `boot.jsonl` (a client comes up and stays up, with `-PwithoutOptional`) and `server-probe.jsonl` (a dedicated server's queue answers with nobody online) |
| `ui/` | The client's own drawing and screens: `hud-layout.jsonl` (thirst against food and air), `hud-hidden.jsonl` (F1), `hud-death-screen.jsonl`, `config-screen.jsonl`, and `config-showcase.jsonl`, which records the config screen for a GIF rather than checking it |
| `gameplay/` | The mod's own mechanics in a real client: `client-sync.jsonl`, `parched.jsonl`, `loot-and-boil.jsonl`, `waterskin-stack.jsonl`, `hanging-pot.jsonl`, `canteen.jsonl` |
| `integrations/` | One script per optional mod: `create-water.jsonl`, `createfly-waterskin.jsonl`, `farmers-delight.jsonl` (and `-1.20.1`, in NBT), `kaleidoscope-cookery.jsonl`, `supplementaries.jsonl`, `brewin-and-chewin.jsonl`, `cold-sweat.jsonl`, `cultural-delights.jsonl`, `fruits-delight.jsonl`, `expanded-delight.jsonl`, `serene-seasons.jsonl` |
| `integrations/sophisticated/` | Sophisticated Backpacks and Storage, one script per upgrade, and `sophisticated-pack/`, the data pack of backpack templates those scripts give out |

## A world for a script

A client script runs in a throwaway world, made fresh for each run:

```bash
python tools/agent/new_world.py 1.21.1 BrewinAgent
```

It reuses the seed and spawn point of another world of that node, and leaves out what a copied
`level.dat` used to bring along: the old player, who arrived mid air or under water and died during the
opening wait. The player now stands on the spawn point, at noon, in clear weather, with no mobs
spawning. Difficulty is left alone, since Peaceful refills thirst. `--datapack <folder>` adds a data
pack; the tool's docstring has the rest. `nbt.py` is the NBT reader and writer it uses, with no
dependency.

So a script needs no `kill` and `client.respawn` at the start; `client.state` expecting
`result.alive` is enough to show the world came up.

## Adding a script

- Put it in the folder its subject belongs to. A new optional mod gets its script in
  `integrations/`, or a folder of its own there once it has more than one.
- Start with a header: what it proves, the nodes it needs, the world (`-Pquickplay=<world>` and the
  `new_world.py` line that makes it), and what to read afterwards when the file cannot say it with `expect` and `checks`.
- Reference it from the `AGENTS.md` or `docs/dev` page of the area it checks, by its full
  `tools/agent/<folder>/<name>.jsonl` path, so a move shows up in a search.
- Prefer a gametest when the check needs no client and no real time. A script is for what
  [src/gametest](../../src/gametest/java/AGENTS.md) cannot reach: rendering, screens, the real right
  click, other mods, and the client's view of synced values.
- Delete a script once a gametest covers the same thing, and remove its references with it.
