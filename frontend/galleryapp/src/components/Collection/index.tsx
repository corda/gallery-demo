import styles from "./styles.module.scss";
import { EmptyState } from "@r3/r3-tooling-design-system";
import { GalleryLot } from "@Models";

interface Props {
  lots: GalleryLot[];
}

function Collection({ lots }: Props) {
  return (
    <section className={styles.main}>
      <h3>Collection</h3>
      {lots.length ? (
        <div className={styles.gallery}>
          {lots
              .sort((a, b) => {
                  return a.description < b.description ? -1 : 1;
              })
              .map((lot, i) => (
            <img
              key={lot.artworkId}
              className={styles.galleryImage}
              src={lot.url}
              alt={lot.description}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon="FolderEdit"
          show
          title="There are no pieces of digital art in your collection"
        />
      )}
    </section>
  );
}

export default Collection;
