export interface Wallet {
  id: string;
  symbol: string;
  encumberedFunds: number;
  availableFunds: number;
}

export enum ParticipantType {
  GalleryOwner,
  Patron,
}

export interface Participant {
  displayName: string;
  id: string;
  wallets: Wallet[];
  type: ParticipantType;
}

export enum BidStatus {
  open,
  placed,
  rejected,
}

export interface GalleryLot {
  id: string;
  displayName: string;
  reservePrice: number;
  currencySymbol: string;
  bids: Bid[];
}

export interface Bid {
  id: string;
  patronId: string;
  patronDisplayName: string;
  value: number;
  currencySymbol: string;
  network: string;
  expiryDate: string;
  status: string;
}
