const artworks = require("./artwork.json");
const balance  = require("./balance.json");
const log = require("./log.json");
const participant = require("./participant.json");
const express = require("express");
const cors = require("cors");
const app = express();

app.use(cors())

app.listen(1337, () => {
    console.log("Server running on port 1337");
});
app.get('/gallery/list-available-artworks', function(req, res) {
   res.json(artworks);
});
app.get('/network/balance', function(req, res) {
    res.json(balance);
});
app.get('/network/log', function(req, res) {
    res.json(log.slice(req.query.index));
});
app.get('/network/participants', function(req, res) {
    res.json(participant);
});