export interface Config {
  gallery: string;
  apiHost: string;
  networks: {
    [k: string]: {
      color: string;
    };
  };
}

const config: Config = {
  gallery: "gallery-id",
  apiHost: process.env.REACT_APP_API_HOST
    ? process.env.REACT_APP_API_HOST
    : "http://gallery-webappapi.cordapayments-sandbox.com",
  networks: {
    AUCTION: {
      color: "#00a37e",
    },
    CBDC: {
      color: "#F7DC6F",
    },
    GBP: {
      color: "#C1B3F3",
    },
  },
};

export default config;
