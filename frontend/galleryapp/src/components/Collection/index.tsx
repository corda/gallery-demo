import styles from "./styles.module.scss";
import { EmptyState } from "@r3/r3-tooling-design-system";

function Collection() {
  return (
    <section className={styles.main}>
      <h2>Collection</h2>

      <EmptyState
        icon="FolderEdit"
        show
        title="There are no pieces of digital art in your collection"
      />
    </section>
  );
}

export default Collection;
