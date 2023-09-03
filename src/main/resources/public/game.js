function connectToWs(wsAddress) {
    const socket = new WebSocket(wsAddress);

    configureWs()

    function configureWs() {
        const handlers = newGameEventHandlers()
        const body = document.querySelector("body")
        const biddingForm = document.createElement("sk-biddingform")
        const bidsEl = document.createElement("sk-bids")

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        socket.addEventListener("message", (event) => {
            console.log("Message from server ", event.data);

            try {
                const data = JSON.parse(event.data)
                switch (data.type) {
                    case "GameEvent$PlayerJoined":
                        return handlers.playerJoined(data);
                    case "GameEvent$GameStarted":
                        return handlers.gameStarted(data);
                    case "GameEvent$RoundStarted":
                        return handlers.roundStarted(data);
                    case "GameEvent$BidPlaced":
                        return handlers.bidPlaced(data);
                    case "GameEvent$BiddingCompleted":
                        return handlers.biddingCompleted(data);
                    case "GameEvent$CardPlayed":
                        return handlers.cardPlayed(data);
                    case "GameEvent$TrickCompleted":
                        return handlers.trickCompleted(data);
                    case "GameEvent$TrickStarted":
                        return handlers.trickStarted(data);
                    case "GameEvent$GameCompleted":
                        return handlers.gameCompleted(data);
                    default: {
                        socket.send(JSON.stringify({
                            type: "ClientMessage$UnhandledMessageFromServer",
                            offender: data.type,
                        }))
                        console.error(`Unknown message from server: ${data.type}`)
                    }
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        });

        function newGameEventHandlers() {

            function updateGamePhase(gamePhase) {
                const gamePhaseEl = document.getElementById("gamePhase")
                const gamePhaseMapping = {
                    "Bidding": "Place your bid!",
                    "TrickTaking": "It's trick taking time!",
                    "TrickComplete": "Trick completed :)"
                }

                const text = gamePhaseMapping[gamePhase]
                if (!text) throw new Error("Unknown game phase: " + gamePhase)
                gamePhaseEl.innerText = gamePhaseMapping[gamePhase]
            }

            return {
                playerJoined(gameEvent) {
                    const players = document.getElementById("players")
                    const li = document.createElement("li")
                    li.innerText = gameEvent.playerId
                    players.appendChild(li)

                    // TODO: I don't think I need this anymore? Or I should have a message saying waiting to start
                    if (!gameEvent.waitingForMorePlayers) {
                        const gameStateEl = document.getElementById("gameState")
                        gameStateEl.innerText = ""
                    }
                },
                gameStarted(gameEvent) {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game has started :D"
                    body.appendChild(biddingForm)

                    body.appendChild(bidsEl)
                    bidsEl.gameStarted(gameEvent.players)
                },
                roundStarted(gameEvent) {
                    const roundNumberEl = document.getElementById("roundNumber")
                    roundNumberEl.innerText = gameEvent.roundNumber
                    updateGamePhase("Bidding")

                    const trickNumberEl = document.getElementById("trickNumber")
                    trickNumberEl.innerText = ""

                    biddingForm.handleRoundStarted()

                    const trick = document.getElementById("trick")
                    trick.textContent = ""

                    const handEl = document.getElementById("hand")
                    gameEvent.cardsDealt.forEach(card => {
                        const li = document.createElement("li")
                        li.innerText = card.id

                        const button = document.createElement("button")
                        button.innerText = "Play"
                        button.onclick = function playCard() {
                            li.remove()
                            socket.send(JSON.stringify({
                                type: "ClientMessage$CardPlayed",
                                cardId: card.id,
                            }))
                        }
                        li.appendChild(button)
                        handEl.appendChild(li)
                    })
                },
                bidPlaced({playerId}) {
                    bidsEl.handleBidPlaced(playerId)
                },
                biddingCompleted({bids}) {
                    updateGamePhase("TrickTaking")
                    bidsEl.handleBiddingCompleted(bids)
                },
                cardPlayed(gameEvent) {
                    const trick = document.getElementById("trick")
                    const li = document.createElement("li")
                    li.innerText = `${gameEvent.playerId}:${gameEvent.cardId}`
                    trick.appendChild(li)
                },
                trickStarted(gameEvent) {
                    const trickNumberEl = document.getElementById("trickNumber")
                    trickNumberEl.innerText = gameEvent.trickNumber

                    const trick = document.getElementById("trick")
                    trick.textContent = ""
                },
                trickCompleted(gameEvent) {
                    updateGamePhase("TrickComplete")
                },
                gameCompleted(gameEvent) {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game is over!"
                },
            }
        }
    }

    class BiddingForm extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.innerHTML = `
                <label>Bid <input type="number" name="bid" min="0" max="10"></label>
                <button id="placeBid" type="button" onclick="onBidSubmit()">Place Bid</button>
                <p id="biddingError"></p>
            `

            const bidInput = this.querySelector(`input[name="bid"]`)
            this.querySelector("#placeBid").onclick = function () {
                socket.send(JSON.stringify({
                    type: "ClientMessage$BidPlaced",
                    bid: bidInput.value,
                }))
            }
        }

        handleRoundStarted() {
            this.querySelector(`input[name="bid"]`).value = ""
        }
    }

    class Bids extends HTMLElement {
        constructor() {
            super();
        }

        connectedCallback() {
            this.innerHTML = `
                <section id="bidsArea">
                    <h3>Bids</h3>
                    <ul id="bids"></ul>
                </section>
            `
        }

        gameStarted(playerIds) {
            const bids = this.querySelector("#bids")
            playerIds.forEach(playerId => {
                const li = document.createElement("li")
                li.textContent = playerId
                li.setAttribute("data-playerBid", playerId)
                li.appendChild(document.createElement("span"))
                bids.appendChild(li)
            })
        }

        handleBidPlaced(playerId) {
            this.querySelector(`[data-playerBid="${playerId}"] span`).innerText = ":" + "has bid"
        }

        handleBiddingCompleted(bids) {
            this.querySelectorAll(`[data-playerBid]`).forEach(el => {
                const playerId = el.getAttribute("data-playerBid")
                const bid = bids[playerId]
                el.querySelector("span").innerText = ":" + bid
            })
        }
    }

    customElements.define("sk-biddingform", BiddingForm)
    customElements.define("sk-bids", Bids)
}
