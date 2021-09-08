const template = [
  {
    id: "f2_1",
    type: "flow",
    data: {
      title: "Consideration State",
      networkType: "considerationLedger",
      properties: {
        assetType: "XDC",
        issuer: "ConsiderationCorp",
        amount: "250",
        owner: "C(Bob_Con)",
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
    id: "f2_2",
    type: "block",
    data: {
      label: "TX 1",
    },
    position: { x: 350, y: 150 },
  },
  {
    id: "f2_3",
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
    id: "f2_4",
    type: "block",
    data: {
      label: "Lock",
      networkType: "green",
      handles: [
        {
          type: "source",
          position: "right",
        },
      ],
    },
    position: { x: 350, y: 234 },
  },
  {
    id: "f2_5",
    type: "block",
    data: {
      label: "C(Bob_Con)",
    },
    position: { x: 350, y: 276 },
  },
  {
    id: "f2_6",
    type: "block",
    data: {
      label: "Notary_Con",
    },
    position: { x: 350, y: 318 },
  },

  {
    id: "f2_7",
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
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 700, y: 25 },
  },
  {
    id: "f2_8",
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
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 700, y: 380 },
  },

  { id: "f2_e1-3", source: "f2_1", target: "f2_3", animated: false, arrowHeadType: "arrow" },
  { id: "f2_e3-8", source: "f2_3", target: "f2_7", animated: false, arrowHeadType: "arrow" },
  { id: "f2_e4-9", source: "f2_4", target: "f2_8", animated: false, arrowHeadType: "arrow" },
];

export default template;
