import { GalleryLot, Wallet } from "./models";

export const mockGalleryLots: GalleryLot[] = [
  {
    id: "testId0",
    displayName: "AppleMan",
    reservePrice: 234,
    currencySymbol: "GBP",
    bids: [
      {
        id: "IDF98HY8F745S0",
        patronId: "UUIDF98HY8F745S0",
        patronDisplayName: "Bob GBP",
        value: 250,
        currencySymbol: "GBP",
        network: "NETWORKIDF98HY8F745S0",
        expiryDate: "",
        status: "open",
      },
      {
        id: "ID93UHGBNH838G6",
        patronId: "UUID93UHGBNH838G6",
        patronDisplayName: "Charlie XDC",
        value: 350,
        currencySymbol: "XDC",
        network: "NETWORKIDD93UHGBNH838G6",
        expiryDate: "",
        status: "open",
      },
    ],
  },
  {
    id: "testId1",
    displayName: "AppleMan",
    reservePrice: 234,
    currencySymbol: "GBP",
    bids: [],
  },
  {
    id: "testId2",
    displayName: "AppleMan",
    reservePrice: 234,
    currencySymbol: "GBP",
    bids: [],
  },
  {
    id: "testId3",
    displayName: "AppleMan",
    reservePrice: 234,
    currencySymbol: "GBP",
    bids: [],
  },
];

export const mockWallets: Wallet[] = [
  {
    id: "HU9H76G7G8HJ9",
    symbol: "GBP",
    encumberedFunds: 0,
    availableFunds: 0,
  },
  {
    id: "9FH3GC6GVBGV78",
    symbol: "XDC",
    encumberedFunds: 0,
    availableFunds: 0,
  },
];
