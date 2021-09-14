import React from "react";
import "./index.scss";
import "@r3/r3-tooling-design-system/lib/index.scss";
import { BrowserRouter as Router, Switch, Route } from "react-router-dom";
import Home from "@Pages/Home";
import Gallery from "@Pages/Gallery";
import Bidder from "@Pages/Bidder";
import { UsersProvider } from "@Context/users";
import { WalletsProvider } from "@Context/wallets";
import { LogsProvider } from "@Context/logs";
import { ArtworksProvider } from "@Context/artworks";
import SiteHeader from "@Components/SiteHeader";

function App() {
  return (
    <div className="App">
      <UsersProvider>
        <Router>
          <SiteHeader />
          <LogsProvider>
            <WalletsProvider>
              <ArtworksProvider>
                <Switch>
                  <Route exact path="/">
                    <Home />
                  </Route>
                  <Route path="/gallery/:id">
                    <Gallery />
                  </Route>
                  <Route path="/bidder/:id">
                    <Bidder />
                  </Route>
                </Switch>
              </ArtworksProvider>
            </WalletsProvider>
          </LogsProvider>
        </Router>
      </UsersProvider>
    </div>
  );
}

export default App;
