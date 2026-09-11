# CrownSMP

A Paper 1.21.11 SMP plugin where one player becomes the Crowned raid boss.

## Requirements

- Paper 1.21.11
- Java 21

## Mechanics

### Crown

The Crowned player has:

- 40 HP (20 hearts)
- Resistance II
- Speed II
- Fire Resistance
- Full knockback resistance
- No permanent Strength
- No absorption hearts

### Crown Rage

The Crowned player builds a combo by landing consecutive hits on players.

By default, 5 hits activates Crown Rage for 8 seconds:

- Strength III
- Speed III
- Resistance III

Taking a hit resets the combo. Waiting longer than the configured combo timeout also breaks the combo. Rage then has a cooldown before it can be earned again.

### Bloodlust

When the Crowned player kills another player, they receive Regeneration II for 5 seconds by default.

### Execution

While sneaking, right-click a player within 4 blocks to attempt an Execution. The target must be at or below 20% of their max health. The ability has a 20 second cooldown and deals the configured finishing damage.

### Crown transfer

Killing the Crowned player transfers the Crown to the killer. Environmental or non-player deaths remove the Crown instead.

## Commands

- `/crown set <player>` - Crown a player
- `/crown give <player>` - Same as set
- `/crown remove` - Remove the current Crown
- `/crown info` - Show Crown information
- `/crown reload` - Reload config.yml
- `/crown help` - Show command help

Commands require the `crownsmp.admin` permission, which defaults to server operators.

## Building

The repository includes a GitHub Actions workflow. Every push to `main` and every pull request builds the plugin with Java 21 and publishes the resulting JAR as a workflow artifact.

You can also build locally with:

```bash
gradle build
```

The compiled plugin is placed in `build/libs/`.
