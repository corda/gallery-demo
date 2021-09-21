import {apiCall} from "@Utils";
import {Balance, GalleryLot, Log, Participant, PostBidAcceptanceParams, PostBidParams} from "@Models";

export const getParticipants = () => apiCall<Participant[]>('get', '/network/participants')

export const getLogs = () => apiCall<Log[]>('get', '/network/log');

export const getBalances = () => apiCall<Balance[]>('get', '/network/balance');

export const getGallery = (userDisplayName?: string) => {
    const queryString = userDisplayName ? `?user=${userDisplayName}` : "";
    return apiCall<GalleryLot[]>('get', `/gallery/list-available-artworks${queryString}`)
}

export const postBid = (params: PostBidParams) =>
    apiCall<string, PostBidParams>("post", "/bidder/bid", params);

export const postBidAcceptance = (params: PostBidAcceptanceParams) =>
    apiCall<string, PostBidAcceptanceParams>("post", "/gallery/accept-bid", params);