# Water sickness

Drinking Dirty or Murky water is a risk that grows with the difficulty: a nuisance on Easy, a
setback on Normal, a gamble with your life on Hard. Boiling the water first always avoids it.

**The purification rework changed steps 1 to 4**:
Clean water is safe on every difficulty, Upset Stomach and Poison share one roll, there is no taste
Nausea and no Nausea bursts. A later change (2026-10-07) left Upset Stomach blocking healing with no
switch and cramping for half a heart at random times, down to half a heart like Poison; it no longer drains
thirst or cuts saturation and quenched, and milk cures it. The numbers in game are in
[WATER-REFERENCE.md](WATER-REFERENCE.md#drinking-bad-water); the tables below for steps 1 to 4 are the
old design, kept for steps 5 onward to build on.

This page is the design. How to build it is in
[WATER-SICKNESS-IMPLEMENTATION.md](WATER-SICKNESS-IMPLEMENTATION.md).

## Steps

| Step | What | Status |
|---|---|---|
| 1a-1d | No Hunger from bad water, the Parched effect, sea water makes you Parched, longer Nausea | done |
| 2 | Upset Stomach | done |
| 3 | Poisoning | done |
| 4 | One roll per drink, by difficulty and grade | done, without Dysentery's range until step 5 |
| 5 | Dysentery, deadly on Hard | planned |
| 5b | Sick vision | planned |
| 6 | Incubation: illness starts later, not on the sip | idea |
| 7 | Salt and oral rehydration salts | planned |
| 8 | Toxins in stagnant water | idea |

- 2, 3 and 4 ship together: they replace the old Nausea and Poison roll.
- 5, 5b and 7 ship together. Dysentery can kill by default, so it never ships without its treatment.

## By difficulty

Pure water is always safe. Salt water is unchanged: no water, Nausea and Parched.

| | Peaceful | Easy | Normal | Hard |
|---|---|---|---|---|
| Taste (7 s Nausea, Dirty and Murky) | yes | yes | yes | yes |
| Upset Stomach | no | level I | level I or II | level I or II |
| Poisoning | no | mild | yes | strong |
| Dysentery | no | no | yes, stops at half a heart | yes, **can kill** |
| Ill from one Dirty bottle | 0% | 65% | 80% | 90% |

## Chance per drink

One roll per drink picks at most one illness, the worst first.
Read as Dysentery / Poisoning / Upset Stomach.

| | Dirty | Murky | Clean |
|---|---|---|---|
| Peaceful | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Easy | 0 / 15 / 50 (I) | 0 / 5 / 30 (I) | 0 / 0 / 5 (I) |
| Normal | 5 / 25 / 50 (II) | 1 / 10 / 40 (I) | 0 / 2 / 10 (I) |
| Hard | 12 / 33 / 45 (II) | 4 / 20 / 46 (II) | 0.5 / 5 / 15 (I) |

- Drinking again while ill rolls again. A worse result replaces the illness, the same one extends
  it (up to twice its time), a milder one does nothing.
- Every fresh drink still quenches. The illness is the price.
- No warning on the tooltip: the grade (Dirty, Murky, Clean, Pure) is the warning.

## The illnesses

### Upset Stomach

The common one. Icon: a red stomach with a green bubble and drop.

- The thirst bar drains faster.
- The screen warps now and then (10 s of Nausea, whose drain Upset Stomach's own replaces).
- Food gives less saturation, so you get hungry sooner. Not hungrier: the appetite goes.
- Never hurts you directly.

| | Easy | Normal | Hard |
|---|---|---|---|
| Lasts | 45 s | 60 s | 90 s |
| Nausea bursts | 1 a minute | I: 1, II: 2 a minute | the same |
| Saturation | ×0.75 | I: ×0.75, II: ×0.5 | the same |

Costs about 2.4 thirst at level I and 4.8 at level II. A bottle gives 6.

### Poisoning

A bad batch. Vanilla effects on top of Upset Stomach. Milk cures it.

| | Easy | Normal | Hard |
|---|---|---|---|
| Poison | 10 s | 20 s | 30 s |

Weakness, Mining Fatigue and Slowness were in the first version and were dropped: the illness should hurt, not
stop the player working.

Vanilla Poison stops at half a heart, so Poisoning never kills.

### Dysentery

The dangerous one. Normal and Hard only.

1. 10 seconds after the drink: *"You feel very ill..."*, a sound, and the icon.
2. The thirst bar drains very fast: a full bar lasts about 2 minutes. Upset Stomach's symptoms come
   with it.
3. The world dims in waves, like Darkness (see Sick vision).
4. **At half thirst or below, fever hurts**: half a heart every 10 seconds. At zero thirst,
   dehydration hurts too.
5. On Normal it stops at half a heart. **On Hard it can kill**: *"Steve died of dysentery"*.
6. Milk does not cure it. It ends when its time runs out.

| | Normal | Hard |
|---|---|---|
| Lasts | 3 min | 5 min |
| Thirst lost over the whole illness | about 27 (4-5 bottles) | about 45 (7-8 bottles) |

## Treatment

| What | Does |
|---|---|
| **Pure water** | Keeps you alive. Above half thirst the fever does nothing, so keep drinking. |
| **Oral rehydration salts** (Pure water + sugar + salt, step 7) | More water than a bottle, halves the time left on Dysentery and Upset Stomach, stops the fever for 60 s. |
| Milk | Cures Poisoning. Not Dysentery. |
| Totem of Undying | Saves you, as always. |

This is the real treatment too: people with this kind of illness die of lost water, and replacing
it saves them.

## How likely is death

Death is never rolled. The roll only decides whether you catch Dysentery; whether it kills depends on
what you do.

On Hard, if you ignore it, it kills in about **2.5 minutes**: half thirst at about 1 minute, fever
starts; empty bar at about 2 minutes; dehydration and fever finish you about 20 seconds later.

| One drink on Hard | Catch Dysentery | Die if you ignore it | Die if you keep drinking Pure water |
|---|---|---|---|
| Dirty | 12% | about 12% | about 0% |
| Murky | 4% | about 4% | about 0% |
| Clean | 0.5% | about 0.5% | about 0% |

It adds up: five Dirty bottles on Hard give about a 47% chance of catching it at least once.

`sicknessCanKill` is **on** by default. Turned off, Dysentery stops at half a heart on every
difficulty.

## Sick vision

What being ill looks like. No vanilla effect is applied: nothing extra in the effect list, nothing
for milk to remove.

| | On screen |
|---|---|
| Upset Stomach | nothing extra, Nausea is enough |
| Poisoning | dark edges; on Hard they pulse |
| Dysentery | Darkness's own light pulse: the world dims every 4 seconds, at 60%; at half thirst or below, 100% |
| Fever hit | the edges dip to near black for half a second |
| Hard, 2 hearts or less | dark edges on top, as if about to faint |

- Follows vanilla's Darkness Effect slider. At 0 it is off.
- No fog for now: fog at 15 blocks hides mobs. Try without it first.

## Config

- The three chance tables, by difficulty and grade.
- The durations and levels of each illness.
- `sicknessCanKill`: on by default.
- `sicknessEffects`, built in place of the three tables and the presets: per difficulty and grade, a
  list of any effect id with its chance, seconds and level, each line rolling on its own. The defaults
  began as this page's numbers, with Poisoning's chance added to Upset Stomach's, since Poisoning
  brought Upset Stomach with it; later Poison's chance went up on every grade that gives it (Clean
  included), and Clean's Upset Stomach and Poison got shorter. The one-roll walk and raising Upset
  Stomach I to II are gone. `extendSicknessEffects` (on) keeps extending: an effect the player has adds
  the line's time, up to twice it, at the higher level; off, vanilla keeps the stronger or longer
  instance. `sicknessPreset`, with the old Classic roll, was removed with the tables.
- `sickVision`: on by default, client side.

## Why it is realistic

| Real | In game |
|---|---|
| Bad water makes you ill, and the danger is losing water | illnesses drain the thirst bar |
| An upset stomach kills the appetite | less saturation |
| A bad batch makes you sick for a while | Poison, longer on harder difficulties |
| Diarrhoeal disease from unsafe water kills hundreds of thousands a year, through dehydration | Dysentery hurts only while you are dry |
| Replacing lost water is the treatment; oral rehydration salts save millions | Pure water keeps you alive, ORS cures faster |
| The dirtier the water, the likelier the illness | chances rise from Clean to Dirty |

A game day is 20 minutes, so real illnesses of days become minutes here.

## Later

- **Incubation (6):** illness starts 1-3 minutes after the drink, with a warning.
- **Toxins (8):** Poisoning likelier from stagnant or hot-biome water; boiling does not clear it,
  charcoal does.

## Open questions

- Name: "Dysentery" (a real disease, and a well-known game death) or a plain "Water Fever".
- Fog for Dysentery on Hard at 2 hearts or less, if the light pulse alone is not scary enough.
- A green thirst bar while ill, like Parched's sand bar.
- Whether you can catch it drinking by hand at full thirst.
