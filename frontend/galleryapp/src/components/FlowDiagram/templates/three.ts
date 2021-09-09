const template = [
  {
    id: "f3_1",
    type: "flow",
    data: {
      title: "Consideration State",
      networkType: "considerationLedger",
      properties: {
        assetType: "XDC",
        issuer: "ConsiderationCorp",
        amount: "250",
        owner: "CompositeKey (C(Bob_Con), C(Alice_Con), threshold 1)",
      },
      participants: ["C(Bob_Con)"],
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
    id: "f3_2",
    type: "flow",
    data: {
      title: "Encumbrance State",
      networkType: "considerationLedger",
      properties: {
        creator: "C(Bob_Con)",
        eventualOwner: "C(Alice_Con)",
        txhash: "signableDate",
        timeWindow: "end 2pm",
      },
      participants: ["C(Bob_Con)", "C(Alice_Con)"],
      handles: [
        {
          type: "source",
          position: "right",
        },
      ],
    },
    position: { x: 0, y: 380 },
  },

  {
    id: "f3_3",
    type: "block",
    data: {
      label: "TX 3",
    },
    position: { x: 350, y: 150 },
  },
  {
    id: "f3_4",
    type: "block",
    data: {
      label: "Transfer",
      networkType: "blue",
      handles: [
        {
          type: "source",
          position: "right",
        },
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 350, y: 192 },
  },
  {
    id: "f3_5",
    type: "block",
    data: {
      label: "Unlock",
      networkType: "green",
      handles: [
        {
          type: "source",
          position: "right",
        },
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 350, y: 234 },
  },
  {
    id: "f3_6",
    type: "block",
    data: {
      label: "C(Bob_Con)",
    },
    position: { x: 350, y: 276 },
  },
  {
    id: "7",
    type: "block",
    data: {
      label: "Notary_Con",
    },
    position: { x: 350, y: 318 },
  },
  {
    id: "f3_8",
    type: "flow",
    data: {
      title: "Consideration State",
      networkType: "considerationLedger",
      properties: {
        assetType: "XDC",
        issuer: "ConsiderationCorp",
        amount: "250",
        owner: "C(Alice_Con)",
      },
      participants: ["C(Alice_Con)"],
      handles: [
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 700, y: 25 },
  },

  { id: "f3_e1-4", source: "f3_1", target: "f3_4", animated: false, arrowHeadType: "arrow" },
  { id: "f3_e2-5", source: "f3_2", target: "f3_5", animated: false, arrowHeadType: "arrow" },
  { id: "f3_e4-8", source: "f3_4", target: "f3_8", animated: false, arrowHeadType: "arrow" },
];

export default template;
