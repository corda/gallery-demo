import { Bid, GalleryLot, Participant } from "@Models";
import axios from "axios";
import config from "@Config";

export function getParticipantPath(participant: Participant): string {
  return `/${participant.type.toLocaleLowerCase()}/${convertToKebabCase(participant.displayName)}`;
}

export function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function convertToKebabCase(string: string) {
  return string.replace(/\s+/g, "-").toLowerCase();
}

export function lotSold(lot: GalleryLot): boolean {
  return lot.bids
    .map((bid) => bid.accepted)
    .reduce((accumulatedStatus, currentStatus) => accumulatedStatus || currentStatus, false);
}

export function usersBid(lot: GalleryLot, user: Participant): Bid | undefined {
  return lot.bids.find((bid) => bid.bidderDisplayName === user.x500);
}

export function getWinningBid(lot: GalleryLot): Bid | undefined {
  return lot.bids.find((bid) => bid.accepted);
}

export async function apiCall<T, U = void>(
  type: "get" | "post",
  path: string,
  params?: U
): Promise<T | null> {
  try {
    const response = await axios[type](`${config.apiHost}${path}`, params);
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
}
