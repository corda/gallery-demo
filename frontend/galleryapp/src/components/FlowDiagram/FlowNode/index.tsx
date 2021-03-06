import { Handle } from "react-flow-renderer";
import styles from "./styles.module.scss";
import { FlowNodeData } from "@Models";

interface Props {
  data: FlowNodeData;
}

const FlowNode = ({ data }: Props) => {
  return (
    <div
      className={`${styles.main} ${styles[data.networkType]}`}
      onMouseDown={(event) => event.stopPropagation()}
    >
      {data.handles &&
        data.handles.map(({ position, type }) => (
          <Handle
            key={`${position}-${type}`}
            type={type}
            position={position}
            style={{ opacity: "0" }}
          />
        ))}
      <h5 className={styles.title}>{data.title}</h5>
      <ul className={styles.properties}>
        <li>
          <h6>Properties</h6>
        </li>
        {Object.entries(data.properties).map((properties) => {
          const [key, value] = properties;
          return (
            <li key={key} className={styles.propertyItem}>
              <div className={styles.propertyKey}>{key}:</div>{" "}
              <div className={styles.propertyValue}>{value}</div>
            </li>
          );
        })}
      </ul>
      <ul className={styles.properties}>
        <li>
          <h6>Participants</h6>
        </li>
        {data.participants.map((participant) => {
          return (
            <li key={participant} className={styles.propertyItem}>
              <div className={styles.propertyValue}>{participant}</div>
            </li>
          );
        })}
      </ul>
    </div>
  );
};

export default FlowNode;
