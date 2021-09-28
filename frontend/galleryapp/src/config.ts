export interface Config {
  gallery: string;
  apiHost: string;
  networkColours: string[];
}

const config: Config = {
  gallery: "gallery-id",
  apiHost: process.env.REACT_APP_API_HOST
    ? process.env.REACT_APP_API_HOST
    : "http://localhost:1337",
  networkColours: ["#00a37e", "#F7DC6F", "#C1B3F3", "#f04a50", "#b1b3c1", "#b5930b"],
};

export default config;
