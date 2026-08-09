import { useState, useMemo, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import apiClient from "@/api/axios";

export interface Room {
  id: string;
  name: string;
  block: string;
  floor: number;
  number: number | string;
  capacity: number;
  internalCapacity: number;
}

export interface FloorGroup {
  floor: number;
  rooms: Room[];
}

export interface BlockGroup {
  block: string;
  floors: FloorGroup[];
  allRooms: Room[];
}

export interface UseRoomSelectionReturn {
  /** Set of currently selected room IDs */
  selectedIds: Set<string>;
  /** Derived count of selected rooms */
  selectedCount: number;
  /** Array of selected room IDs (for submission payload) */
  selectedRoomIds: string[];
  blockGroups: BlockGroup[];
  filteredRooms: Set<string>;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  isLoading: boolean;

  // ─── Selection Actions ────────────────────────────────────
  toggleRoom: (roomId: string) => void;
  selectAll: () => void;
  clearAll: () => void;
  toggleBlock: (block: string) => void;
  toggleFloor: (block: string, floor: number) => void;

  // ─── Derived Checkbox States ──────────────────────────────
  isAllSelected: boolean;
  isAllIndeterminate: boolean;
  isBlockSelected: (block: string) => boolean;
  isBlockIndeterminate: (block: string) => boolean;
  isFloorSelected: (block: string, floor: number) => boolean;
  isFloorIndeterminate: (block: string, floor: number) => boolean;
}

export function useRoomSelection(): UseRoomSelectionReturn {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [searchQuery, setSearchQuery] = useState("");

  const { data: allRooms = [], isLoading } = useQuery<Room[]>({
    queryKey: ["halls", "selector"],
    queryFn: async () => {
      const res = await apiClient.get("/halls");
      return res.data.map((h: { id: string; name?: string; capacity?: number }) => {
        let block = "HALL";
        let floor = 1;
        let number: string | number = h.id;

        if (h.id.includes("-")) {
          const parts = h.id.split("-");
          block = parts[0] || "HALL";
          const rawNum = parts[1];
          const parsedNum = parseInt(rawNum, 10);
          
          if (!isNaN(parsedNum)) {
            number = parsedNum;
            floor = Math.floor(parsedNum / 100);
            if (block === "CONF") floor = 0;
            if (floor === 0 && block !== "CONF") floor = 1;
          } else {
            number = rawNum || h.id;
            floor = 1;
          }
        } else {
          const digitMatch = h.id.match(/\d+/);
          if (digitMatch) {
            const parsedNum = parseInt(digitMatch[0], 10);
            number = parsedNum;
            floor = Math.floor(parsedNum / 100) || 1;
            block = h.id.replace(/\d+/g, "").trim() || "HALL";
          } else {
            block = h.id;
            number = "";
            floor = 1;
          }
        }

        return {
          id: h.id,
          name: h.name || h.id,
          block: block || "HALL",
          floor: typeof floor === "number" && !isNaN(floor) ? floor : 1,
          number: number !== undefined && number !== null ? number : h.id,
          capacity: h.capacity || 25,
          internalCapacity: h.internalCapacity || 40
        };
      });
    },
  });

  const blockGroups = useMemo(() => {
    const blockMap = new Map<string, Map<number, Room[]>>();
    for (const room of allRooms) {
      if (!blockMap.has(room.block)) blockMap.set(room.block, new Map());
      const floorMap = blockMap.get(room.block)!;
      if (!floorMap.has(room.floor)) floorMap.set(room.floor, []);
      floorMap.get(room.floor)!.push(room);
    }
    const groups: BlockGroup[] = [];
    for (const [blockName, floorMap] of blockMap.entries()) {
      const floors: FloorGroup[] = [];
      const sortedFloors = [...floorMap.keys()].sort((a, b) => a - b);
      for (const floor of sortedFloors) {
        floors.push({
          floor,
          rooms: floorMap.get(floor)!.sort((a, b) => {
            const numA = typeof a.number === 'number' ? a.number : 0;
            const numB = typeof b.number === 'number' ? b.number : 0;
            return numA - numB;
          }),
        });
      }
      groups.push({
        block: blockName,
        floors,
        allRooms: floors.flatMap((f) => f.rooms),
      });
    }
    return groups.sort((a, b) => a.block.localeCompare(b.block));
  }, [allRooms]);

  const roomsByBlock = useMemo(() => {
    const map = new Map<string, Room[]>();
    for (const r of allRooms) {
      if (!map.has(r.block)) map.set(r.block, []);
      map.get(r.block)!.push(r);
    }
    return map;
  }, [allRooms]);

  const roomsByBlockFloor = useMemo(() => {
    const map = new Map<string, Room[]>();
    for (const r of allRooms) {
      const key = `${r.block}::${r.floor}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(r);
    }
    return map;
  }, [allRooms]);

  const filteredRooms = useMemo(() => {
    if (!searchQuery.trim()) {
      return new Set(allRooms.map((r: Room) => r.id));
    }
    const q = searchQuery.toLowerCase().trim();
    return new Set(
      allRooms.filter(
        (r: Room) =>
          r.id.toLowerCase().includes(q) ||
          String(r.number).includes(q) ||
          r.block.toLowerCase().includes(q)
      ).map((r: Room) => r.id)
    );
  }, [searchQuery, allRooms]);

  const toggleRoom = useCallback((roomId: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(roomId)) {
        next.delete(roomId);
      } else {
        next.add(roomId);
      }
      return next;
    });
  }, []);

  const selectAll = useCallback(() => {
    setSelectedIds(new Set(allRooms.map((r: Room) => r.id)));
  }, [allRooms]);

  const clearAll = useCallback(() => {
    setSelectedIds(new Set());
  }, []);

  const toggleBlock = useCallback(
    (block: string) => {
      const blockRooms = roomsByBlock.get(block) || [];
      setSelectedIds((prev) => {
        const next = new Set(prev);
        const allSelected = blockRooms.every((r) => next.has(r.id));
        if (allSelected) {
          blockRooms.forEach((r) => next.delete(r.id));
        } else {
          blockRooms.forEach((r) => next.add(r.id));
        }
        return next;
      });
    },
    [roomsByBlock]
  );

  const toggleFloor = useCallback(
    (block: string, floor: number) => {
      const key = `${block}::${floor}`;
      const floorRooms = roomsByBlockFloor.get(key) || [];
      setSelectedIds((prev) => {
        const next = new Set(prev);
        const allSelected = floorRooms.every((r) => next.has(r.id));
        if (allSelected) {
          floorRooms.forEach((r) => next.delete(r.id));
        } else {
          floorRooms.forEach((r) => next.add(r.id));
        }
        return next;
      });
    },
    [roomsByBlockFloor]
  );

  const selectedCount = selectedIds.size;
  const selectedRoomIds = useMemo(() => [...selectedIds], [selectedIds]);

  const isAllSelected = allRooms.length > 0 && selectedIds.size === allRooms.length;
  const isAllIndeterminate = selectedIds.size > 0 && !isAllSelected;

  const isBlockSelected = useCallback(
    (block: string) => {
      const rooms = roomsByBlock.get(block) || [];
      return rooms.length > 0 && rooms.every((r) => selectedIds.has(r.id));
    },
    [selectedIds, roomsByBlock]
  );

  const isBlockIndeterminate = useCallback(
    (block: string) => {
      const rooms = roomsByBlock.get(block) || [];
      const count = rooms.filter((r) => selectedIds.has(r.id)).length;
      return count > 0 && count < rooms.length;
    },
    [selectedIds, roomsByBlock]
  );

  const isFloorSelected = useCallback(
    (block: string, floor: number) => {
      const key = `${block}::${floor}`;
      const rooms = roomsByBlockFloor.get(key) || [];
      return rooms.length > 0 && rooms.every((r) => selectedIds.has(r.id));
    },
    [selectedIds, roomsByBlockFloor]
  );

  const isFloorIndeterminate = useCallback(
    (block: string, floor: number) => {
      const key = `${block}::${floor}`;
      const rooms = roomsByBlockFloor.get(key) || [];
      const count = rooms.filter((r) => selectedIds.has(r.id)).length;
      return count > 0 && count < rooms.length;
    },
    [selectedIds, roomsByBlockFloor]
  );

  return {
    selectedIds,
    selectedCount,
    selectedRoomIds,
    blockGroups,
    filteredRooms,
    searchQuery,
    setSearchQuery,
    toggleRoom,
    selectAll,
    clearAll,
    toggleBlock,
    toggleFloor,
    isAllSelected,
    isAllIndeterminate,
    isBlockSelected,
    isBlockIndeterminate,
    isFloorSelected,
    isFloorIndeterminate,
    isLoading
  };
}
