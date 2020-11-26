# owl-audit

## Ruby

<details>
<summary>Details</summary>

The audit framework was originally written in jRuby 1.7

To install:

1) Create a file: `~/.rvmrc` with the following:

```
rvm_silence_path_mismatch_check_flag=1
rvm_ignore_gemsets_flag=1
```

2) Install RVM

```zsh
curl -sSL https://get.rvm.io | bash -s stable --ruby=jruby-1.7 --without-gems="gem-wrappers rubygems-bundler rake bundler"
```

3) Update `~/.zshrc`

```zsh
export PATH="$PATH:$HOME/.rvm/bin"
source $HOME/.rvm/scripts/rvm
```

4) To use:

```zsh
rvm use jruby-1.7.27
```

</details>
