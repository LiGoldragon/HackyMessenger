# Hacky Messenger

Hacky Messenger (HM) is the hand-built flow messaging and registry stack. It
binds a Flow ID to one live Herdr pane and prompts that pane through `herdr
agent prompt`. It is compensation: welded in place to make the system run until
Flow Nexus and Message Nexus take over. Requires Python 3, Bash, Herdr, and
Orchestrate.

## Usage is documented in a skill

The commands are documented by the compensation skill
`compensation-hacky-messenger`, authored at
`skills/compensation-hacky-messenger.md` in
[Curriculum](https://github.com/LiGoldragon/Curriculum). Every command's
`--help` names it. **Whoever changes this tool updates that skill in the same
landing.**

## Commands

`bin/hm-send`, `bin/hm-send-abrupt`, `bin/hm-list`, `bin/hm-register`,
`bin/hm-rebind`, `bin/hm-move`, and `bin/hm-retire` each run one `hm.py`
subcommand. `hm.py deregister` and `hm.py import-retirement` have no wrapper.

## Installation

Not packaged by Nix. The commands on `PATH` are symbolic links in
`~/.local/bin`, one per wrapper, pointing into this checkout:

```sh
for command in /git/github.com/LiGoldragon/HackyMessenger/bin/hm-*; do
  ln -sfn "$command" ~/.local/bin/
done
```

A wrapper resolves its own link, so the checkout at
`/git/github.com/LiGoldragon/HackyMessenger` is what runs. Registration
state lives outside Git in `~/.local/state/hacky-messenger` (or `HM_REGISTRY`).

This repository was extracted, with its history, from `tools/hacky-messenger`
and `tools/hm-*` in the primary workspace. The primary workspace still holds
those copies for tools that call them by path.

## Checks

```sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s . -v
```

`check.nix { inherit pkgs; }` runs the same tests as a Nix check. The tests
fake Herdr.
