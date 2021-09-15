import { FlowData } from "@Models";

const labelStyle = {
  "font-size": "14px",
  "font-weight": "bold",
  fill: "#49505f",
  "font-family": "Monaco",
};

const template = (data: FlowData) => [
  {
    id: "f4_1",
    type: "flow",
    data: {
      title: "NFT State",
      networkType: "nftLedger",
      properties: data.states[0] ? data.states[0].properties : {},
      participants: data.states[0] ? data.states[0].participants : [],
      handles: [
        {
          type: "source",
          position: "right",
        },
      ],
    },
    position: { x: 0, y: 25 },
  },
  {
    id: "f4_2",
    type: "block",
    data: {
      label: "draft Tx",
    },
    position: { x: 450, y: 50 },
  },
  {
    id: "f4_3",
    type: "block",
    data: {
      networkType: "green",
      label: "Transfer",
      handles: [
        {
          type: "source",
          position: "right",
        },
        {
          type: "target",
          position: "left",
        },
        {
          type: "target",
          position: "bottom",
        },
      ],
    },
    position: { x: 450, y: 99 },
  },
  {
    id: "f4_4",
    type: "block",
    data: {
      label: Object.keys(data.signers)[0],
      handles: [
        {
          type: "source",
          position: "top",
        },
      ],
    },
    position: { x: 450, y: 300 },
  },
  {
    id: "f4_5",
    type: "flow",
    data: {
      title: "NFT State",
      networkType: "nftLedger",
      properties: data.states[1] ? data.states[1].properties : {},
      participants: data.states[1] ? data.states[1].participants : [],
      handles: [
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 800, y: 25 },
  },

  { id: "f4_e1-3", source: "f4_1", target: "f4_3", animated: false, arrowHeadType: "arrow" },
  {
    id: "f4_e4-3",
    source: "f4_4",
    target: "f4_3",
    animated: true,
    targetHandle: "bottom",
    label: "Required Sigs",
    labelBgStyle: { fill: "#f8f8f8" },
    labelStyle,
  },
  { id: "f4_e3-5", source: "f4_3", target: "f4_5", animated: false, arrowHeadType: "arrow" },
];

export default template;
