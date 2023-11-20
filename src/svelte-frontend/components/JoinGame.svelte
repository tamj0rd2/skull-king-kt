<script lang="ts">
    import {messageStore} from "../socket";
    import {PlayerCommand} from "../generated_types";

    let playerIdInput = ""
</script>

<form id="joinGame"
      on:submit|preventDefault={() => messageStore.send({type: PlayerCommand.Type.JoinGame, actor: playerIdInput})}>
    <label>
        Player ID:
        <input bind:value={playerIdInput} name="playerId" placeholder="Enter a name"/>
    </label>
    <input type="submit" value="Submit"/>
</form>
