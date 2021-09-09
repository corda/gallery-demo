import styles from "./styles.module.scss";
import ParticipantSelection from "@Components/ParticipantSelection";
import { UsersContext } from "@Context/users";
import { useContext } from "react";
import PageContainer from "@Components/PageContainer";

function Home() {
  const { list } = useContext(UsersContext);

  return (
    <PageContainer>
      <div className={styles.main}>
        <h1>Select a participant</h1>
        {list && <ParticipantSelection participants={list} />}
      </div>
    </PageContainer>
  );
}

export default Home;
