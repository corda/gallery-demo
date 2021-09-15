import { Column, Container, Row } from "@r3/r3-tooling-design-system";
import Wallets from "@Components/Token";
import GalleryShop from "@Components/GalleryShop";
import Collection from "@Components/Collection";
import { useParams } from "react-router-dom";
import { RouterParams } from "@Models";
import { useContext } from "react";
import { UsersContext } from "@Context/users";
import PageContainer from "@Components/PageContainer";
import { ArtworksContext } from "@Context/artworks";

function Bidder() {
  const { id } = useParams<RouterParams>();
  const { getUser } = useContext(UsersContext);
  const { artworks } = useContext(ArtworksContext);
  const user = getUser(id);
  const biddersArt = artworks.filter((lot) => {
    return !!lot.bids.find((bid) => bid.accepted && bid.bidderDisplayName === user?.displayName);
  });

  return (
    <PageContainer>
      <Container>
        {user && (
          <Row>
            <Column cols={8}>
              <GalleryShop lots={artworks} x500={user.x500} />
            </Column>
            <Column cols={4}>
              <Wallets user={user} />
              <Collection lots={biddersArt} />
            </Column>
          </Row>
        )}
      </Container>
    </PageContainer>
  );
}

export default Bidder;
