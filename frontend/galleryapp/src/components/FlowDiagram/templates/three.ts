import { FlowData } from "@Models";

const template = (data: FlowData) => [
  {
    id: "f3_1",
    type: "flow",
    data: {
      title: "Consideration State",
      networkType: "considerationLedger",
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
    id: "f3_2",
    type: "flow",
    data: {
      title: "Encumbrance State",
      networkType: "considerationLedger",
      properties: data.states[1] ? data.states[1].properties : {},
      participants: data.states[1] ? data.states[1].participants : [],
      handles: [
        {
          type: "source",
          position: "right",
        },
      ],
    },
    position: { x: 0, y: 580 },
  },
  {
    id: "f3_3",
    type: "block",
    data: {
      label: "TX 3",
    },
    position: { x: 450, y: 150 },
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
    position: { x: 450, y: 199 },
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
    position: { x: 450, y: 248 },
  },
  {
    id: "f3_6",
    type: "block",
    data: {
      label: Object.keys(data.signers)[0],
    },
    position: { x: 450, y: 297 },
  },
  {
    id: "7",
    type: "block",
    data: {
      label: Object.keys(data.signers)[1],
    },
    position: { x: 450, y: 346 },
  },
  {
    id: "f3_8",
    type: "flow",
    data: {
      title: "Consideration State",
      networkType: "considerationLedger",
      properties: data.states[2] ? data.states[2].properties : {},
      participants: data.states[2] ? data.states[2].participants : [],
      handles: [
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 800, y: 25 },
  },

  { id: "f3_e1-4", source: "f3_1", target: "f3_4", animated: false, arrowHeadType: "arrow" },
  { id: "f3_e2-5", source: "f3_2", target: "f3_5", animated: false, arrowHeadType: "arrow" },
  { id: "f3_e4-8", source: "f3_4", target: "f3_8", animated: false, arrowHeadType: "arrow" },
];

export default template;
