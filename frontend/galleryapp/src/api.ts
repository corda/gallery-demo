import { apiCall } from "@Utils";
import {
  Balance,
  GalleryLot, GetLogParams,
  Log,
  Participant,
  PostBidAcceptanceParams,
  PostBidParams,
} from "@Models";

export const getParticipants = () => apiCall<Participant[]>("get", "/network/participants");

export const getLogs = (index: number) => apiCall<Log[], GetLogParams>("get", "/network/log", {index});

export const getBalances = () => apiCall<Balance[]>("get", "/network/balance");

export const resetInit = () => apiCall<null>("get", "/network/init");

export const getGallery = (userDisplayName?: string) => {
  const queryString = userDisplayName ? `?user=${userDisplayName}` : "";
  return apiCall<GalleryLot[]>("get", `/gallery/list-available-artworks${queryString}`);
};

export const postBid = (params: PostBidParams) =>
  apiCall<string, PostBidParams>("post", "/bidder/bid", params);

export const postBidAcceptance = (params: {
  artworkId: string;
  currency: string;
  bidderParty: string;
}) => apiCall<string, PostBidAcceptanceParams>("post", "/gallery/accept-bid", params);
