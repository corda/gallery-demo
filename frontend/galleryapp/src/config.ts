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
    : "http://localhost:1337",
  networks: {
    auction: {
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
