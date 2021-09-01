import styles from "./styles.module.scss";
import { GalleryLot } from "../../models";
import CatalogueItem from "@Components/Catalogue/CatalogueItem";

interface Props {
  lots: GalleryLot[];
}

function Catalogue({ lots }: Props) {
  return (
    <section className={styles.main}>
      <h2>Catalogue</h2>
      <table>
        <thead>
          <tr>
            <th />
            <th>Lot</th>
            <th>Lot Id</th>
            <th>Reserve Price</th>
            <th># Bids</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {lots.map((lot) => (
            <CatalogueItem key={lot.id} lot={lot} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

export default Catalogue;
