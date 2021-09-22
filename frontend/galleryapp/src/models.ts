import { Position } from "react-flow-renderer";

export enum ParticipantType {
  GalleryOwner,
  Bidder,
}

export interface NetworkId {
  network: string;
  publicKey: string;
}

export interface Participant {
  displayName: string;
  networkIds: NetworkId[];
  type: string;
  x500: string;
}

export interface Balance {
  artworkParty: string;
  partyBalances: Token[];
  x500: string;
}

export interface Token {
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
  expiryDate: string;
}

export interface Bid {
  cordaReference: string;
  bidderPubKey: string;
  bidderDisplayName: string;
  amount: number;
  currencyCode: string;
  notary: string;
  accepted: boolean;
}

export interface Log {
  logRecordId: string;
  timestamp: string;
  message: string;
  associatedFlow?: string;
  network: string;
  x500: string;
  completed?: FlowData;
}

export interface FlowData {
  associatedStage: string;
  logRecordId: string;
  states: FlowState[];
  signers: { [index: string]: boolean };
}

export interface FlowState {
  properties: { [index: string]: string };
  participants: string[];
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
  position: Position;
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

export interface PostBidParams {
  bidderParty: string;
  artworkId: string;
  amount: string;
  currency: string;
}

export interface PostBidAcceptanceParams {
  bidderParty: string;
  artworkId: string;
  currency: string;
}
