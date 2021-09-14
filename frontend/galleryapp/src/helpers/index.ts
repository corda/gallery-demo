import { GalleryLot, Participant } from "../models";

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
