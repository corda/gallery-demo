const labelStyle = {
  "font-size": "14px",
  "font-weight": "bold",
  fill: "#49505f",
  "font-family": "Monaco",
};

const template = [
  {
    id: "f1_1",
    type: "flow",
    data: {
      title: "NFT State",
      networkType: "draft",
      properties: {
        assetTypeX: "ArtWork",
        issuer: "ArtWorkCorp",
        amount: "1",
        owner: "C(Alice_NFT)",
      },
      participants: ["C(Alice_NFT)"],
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
    id: "f1_2",
    type: "block",
    data: {
      label: "draft Tx",
      networkType: "draft",
    },
    position: { x: 300, y: 50 },
  },
  {
    id: "f1_3",
    type: "block",
    data: {
      networkType: "draft",
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
    position: { x: 300, y: 92 },
  },
  {
    id: "f1_4",
    type: "block",
    data: {
      label: "C(Alice_NFT)",
      networkType: "draft",
      handles: [
        {
          type: "source",
          position: "top",
        },
      ],
    },
    position: { x: 300, y: 300 },
  },
  {
    id: "f1_5",
    type: "flow",
    data: {
      title: "NFT State",
      networkType: "draft",
      properties: {
        assetType: "ArtWork",
        issuer: "ArtWorkCorp",
        amount: "1",
        owner: "C(Bob_NFT)",
      },
      participants: ["C(Bob_NFT)"],
      handles: [
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 600, y: 25 },
  },

  { id: "f1_e1-3", source: "f1_1", target: "f1_3", animated: false, arrowHeadType: "arrow" },
  {
    id: "f1_e4-3",
    source: "f1_4",
    target: "f1_3",
    animated: true,
    targetHandle: "bottom",
    label: "Required Sigs",
    labelBgStyle: { fill: "#f8f8f8" },
    labelStyle,
  },
  { id: "f1_e3-5", source: "f1_3", target: "f1_5", animated: false, arrowHeadType: "arrow" },
];

export default template;
