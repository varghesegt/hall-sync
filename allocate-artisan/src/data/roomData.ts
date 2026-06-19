/**
 * Single source of truth for all room definitions.
 * The entire UI is generated dynamically from this array.
 * To add/remove rooms, modify ONLY this file.
 * Total: 85 rooms — LH (Lecture Hall), SH (Seminar Hall), CONF (Conference Hall)
 */

export interface Room {
  /** Unique room identifier, e.g. "LH-201", "SH-301", "CONF-1" */
  id: string;
  /** Block name: "LH", "SH", or "CONF" */
  block: string;
  /** Floor number (2–5, or 0 for conference halls) */
  floor: number;
  /** Room number (display-friendly) */
  number: number;
}

// ─── Room Generator Helpers ─────────────────────────────────────────────────

function generateRange(
  start: number,
  end: number,
  block: string,
  floor: number
): Room[] {
  const rooms: Room[] = [];
  for (let n = start; n <= end; n++) {
    const id = `${block}-${n}`;
    rooms.push({ id, block, floor, number: n });
  }
  return rooms;
}

function generateSpecific(
  numbers: number[],
  block: string,
  floor: number
): Room[] {
  return numbers.map((n) => ({
    id: `${block}-${n}`,
    block,
    floor,
    number: n,
  }));
}

// ─── ROOM DEFINITIONS ──────────────────────────────────────────────────────

export const ALL_ROOMS: Room[] = [
  // LH Block – 2nd Floor: LH 201–220
  ...generateRange(201, 220, "LH", 2),

  // LH Block – 3rd Floor: LH 301–321
  ...generateRange(301, 321, "LH", 3),

  // SH Block – 3rd Floor: SH 301, 302, 303
  ...generateSpecific([301, 302, 303], "SH", 3),

  // LH Block – 4th Floor: LH 401–420
  ...generateRange(401, 420, "LH", 4),

  // LH Block – 5th Floor: LH 501–515
  ...generateRange(501, 515, "LH", 5),

  // SH Block – 5th Floor: SH 501, 502, 503
  ...generateSpecific([501, 502, 503], "SH", 5),

  // CONF Block – Conference Halls: CONF 1, 2, 3
  ...generateSpecific([1, 2, 3], "CONF", 0),
];

// ─── Pre-computed groupings ─────────────────────────────────────────────────

export interface FloorGroup {
  floor: number;
  rooms: Room[];
}

export interface BlockGroup {
  block: string;
  floors: FloorGroup[];
  allRooms: Room[];
}

/** Group rooms by block → floor, computed once at module load. */
export function getBlockGroups(): BlockGroup[] {
  const blockMap = new Map<string, Map<number, Room[]>>();

  for (const room of ALL_ROOMS) {
    if (!blockMap.has(room.block)) {
      blockMap.set(room.block, new Map());
    }
    const floorMap = blockMap.get(room.block)!;
    if (!floorMap.has(room.floor)) {
      floorMap.set(room.floor, []);
    }
    floorMap.get(room.floor)!.push(room);
  }

  // Order: LH first, then SH, then CONF
  const blockOrder = ["LH", "SH", "CONF"];
  const groups: BlockGroup[] = [];

  for (const blockName of blockOrder) {
    const floorMap = blockMap.get(blockName);
    if (!floorMap) continue;

    const floors: FloorGroup[] = [];
    const sortedFloors = [...floorMap.keys()].sort((a, b) => a - b);

    for (const floor of sortedFloors) {
      floors.push({
        floor,
        rooms: floorMap.get(floor)!.sort((a, b) => a.number - b.number),
      });
    }

    groups.push({
      block: blockName,
      floors,
      allRooms: floors.flatMap((f) => f.rooms),
    });
  }

  return groups;
}

// Validate no duplicate IDs at import time
const ids = ALL_ROOMS.map((r) => r.id);
const uniqueIds = new Set(ids);
if (ids.length !== uniqueIds.size) {
  console.error(
    "[RoomData] Duplicate room IDs detected!",
    ids.filter((id, i) => ids.indexOf(id) !== i)
  );
}
