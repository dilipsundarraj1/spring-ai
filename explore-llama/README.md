<!-- TOC -->
* [Ollama](#ollama)
  * [Install ollama](#install-ollama)
  * [Start ollama](#start-ollama)
  * [Stop ollama](#stop-ollama)
  * [Pull a new model using ollama](#pull-a-new-model-using-ollama)
  * [List all the models in your local](#list-all-the-models-in-your-local)
  * [Run a model in local using ollama](#run-a-model-in-local-using-ollama)
<!-- TOC -->

# Ollama

## Install ollama

```sh
brew install ollama
```

## Start ollama

```sh
brew services start ollama
```

## Stop ollama

```sh
brew services stop ollama
```

## Pull a new model using ollama

```shell
ollama pull llama
```

## List all the models in your local

```shell
ollama ls
```

## Run a model in local using ollama

```shell
ollama run llama3
```

## API to access to Models running in local


- The port that the services are accessible is via the port [11434](https://github.com/ollama/ollama/blob/5296f487a840b2b9ffc28ed9b45d223a32359973/Dockerfile#L125C25-L125C30)
  