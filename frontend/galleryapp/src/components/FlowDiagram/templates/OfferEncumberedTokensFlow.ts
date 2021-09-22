import { FlowData } from "@Models";

const template = (data: FlowData) => [
  {
    id: "f2_1",
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
    id: "f2_2",
    type: "block",
    data: {
      label: "TX 1",
    },
    position: { x: 450, y: 150 },
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
    position: { x: 450, y: 192 },
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
    position: { x: 450, y: 234 },
  },
  {
    id: "f2_5",
    type: "block",
    data: {
      label: Object.keys(data.signers)[0],
    },
    position: { x: 450, y: 276 },
  },
  {
    id: "f2_6",
    type: "block",
    data: {
      label: Object.keys(data.signers)[1],
    },
    position: { x: 450, y: 318 },
  },

  {
    id: "f2_7",
    type: "flow",
    data: {
      title: "Consideration State - Requested",
      networkType: "considerationLedger",
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
  {
    id: "f2_8",
    type: "flow",
    data: {
      title: "Consideration State - Change",
      networkType: "draft",
      properties: data.states[2] ? data.states[2].properties : {},
      participants: data.states[2] ? data.states[2].participants : [],
      handles: [
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 800, y: 450 },
  },
  {
    id: "f2_9",
    type: "flow",
    data: {
      title: "Encumbrance State",
      networkType: "considerationLedger",
      properties: data.states[3] ? data.states[3].properties : {},
      participants: data.states[3] ? data.states[3].participants : [],
      handles: [
        {
          type: "target",
          position: "left",
        },
      ],
    },
    position: { x: 800, y: 880 },
  },

  { id: "f2_e1-3", source: "f2_1", target: "f2_3", animated: false, arrowHeadType: "arrow" },
  { id: "f2_e3-7", source: "f2_3", target: "f2_7", animated: false, arrowHeadType: "arrow" },
  { id: "f2_e3-8", source: "f2_3", target: "f2_8", animated: false, arrowHeadType: "arrow" },
  { id: "f2_e4-9", source: "f2_4", target: "f2_9", animated: false, arrowHeadType: "arrow" },
];

export default template;
