import { Handle } from "react-flow-renderer";
import styles from "./styles.module.scss";
import { NodeHandle } from "@Models";

interface Props {
  data: {
    label: string;
    handles?: NodeHandle[];
    networkType: string;
  };
}

const BlockNode = ({ data }: Props) => {
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
            id={position}
            position={position}
            style={{ opacity: "0" }}
          />
        ))}
      <span className={styles.title}>{data.label}</span>
    </div>
  );
};

export default BlockNode;
