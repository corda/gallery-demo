export enum ParticipantType {
  GalleryOwner,
  Bidder,
}

export interface NetworkId {
  network: string;
  x500: string;
  publicKey: string;
}

export interface Participant {
  displayName: string;
  networkIds: NetworkId[];
  type: string;
}

export interface Balance {
  artworkParty: string;
  balances: Wallet[];
}

export interface Wallet {
  currencyCode: string;
  encumberedFunds: number;
  availableFunds: number;
}

export interface GalleryLot {
  artworkId: string;
  description: string;
  listed: boolean;
  url: string;
  bids: Bid[];
}

export interface Bid {
  cordaReference: string;
  bidderPubKey: string;
  bidderDisplayName: string;
  amount: number;
  currencyCode: string;
  notary: string;
  expiryDate: string;
  accepted: boolean;
}

export interface Log {
  logRecordId: string;
  timestamp: string;
  message: string;
  associatedFlow?: string;
  network: string;
  x500: string;
}

export interface RouterParams {
  id: string;
}

export enum NodeType {
  Consideration = "consideration",
  NFTLedger = "nftLedger",
  Draft = "draft",
}

export interface NodeHandle {
  position: "top" | "right" | "bottom" | "left";
  type: "source" | "target";
}

export interface FlowNodeData {
  title: string;
  networkType: NodeType;
  properties: {
    [index: string]: string;
  };
  participants: string[];
  handles?: NodeHandle[];
}
