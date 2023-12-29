<script lang="ts">
    import {commander, commandIsInProgress, state} from "./socket";
    import Spinner from "./components/Spinner.svelte";
    import {DisplayBid, RoundPhase} from "../generated_types";

    let playerIdInput = ''
    let bidInput = 0

    function displayedBid(displayBid: DisplayBid): string {
        switch (displayBid.type) {
            case DisplayBid.Type.Hidden:
                return "Hidden"
            case DisplayBid.Type.None:
                return "None"
            case DisplayBid.Type.Placed:
                return displayBid.bid.toString()

        }
    }
</script>

{#if !$state}
    <h1>Game Page</h1>

    <form id="joinGame" on:submit|preventDefault={() => $commander.joinGame(playerIdInput)}>
        <input type="text" name="playerId" bind:value={playerIdInput} />
        <input type="submit" value="Join game" />
    </form>
{:else}
    <h1>Game Page - {$state.playerId}</h1>
    <ul id="players">
        {#each $state.playersInRoom as player}
            <li>{player}</li>
        {/each}
    </ul>

    {#if $state.gameState}
        <p id="gameState">{$state.gameState}</p>
    {/if}
    {#if $state.roundPhase}
        <p id="roundPhase">{$state.roundPhase}</p>
    {/if}

    {#if $state.roundPhase === RoundPhase.Bidding}
        <form id="placeBid" on:submit|preventDefault={() => $commander.placeBid(bidInput)}>
            <input type="number" name="bid" bind:value={bidInput} />
            <input type="submit" value="Bid" />
        </form>
    {/if}

    {#if $state.bids}
        <ul id="bids">
            {#each Object.entries($state.bids) as [playerId, bid]}
                <li data-playerId={playerId} data-bid={displayedBid(bid)}>{playerId} - {displayedBid(bid)}</li>
            {/each}
        </ul>
    {/if}

    {#if $commandIsInProgress}
        <Spinner />
    {/if}
{/if}
