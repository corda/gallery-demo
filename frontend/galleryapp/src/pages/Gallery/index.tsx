import { Column, Container, Row } from "@r3/r3-tooling-design-system";
import Catalogue from "@Components/Catalogue";
import Wallets from "@Components/Wallets";
import { useParams } from "react-router-dom";
import { RouterParams } from "@Models";
import { useContext } from "react";
import { UsersContext } from "@Context/users";
import PageContainer from "@Components/PageContainer";
import { ArtworksContext } from "@Context/artworks";

function Gallery() {
  const { id } = useParams<RouterParams>();
  const { getUser } = useContext(UsersContext);
  const { artworks } = useContext(ArtworksContext);
  const user = getUser(id);

  return (
    <PageContainer>
      <Container>
        {user && (
          <Row>
            <Column cols={8}>
              <Catalogue lots={artworks} />
            </Column>
            <Column cols={4}>{user && <Wallets user={user} />}</Column>
          </Row>
        )}
      </Container>
    </PageContainer>
  );
}

export default Gallery;
