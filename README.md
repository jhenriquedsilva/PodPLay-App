# Este é o PodPlay

Ele consome a API do iTunes para pesquisar por podcasts e assisti-los. Ele ainda está incompleto.
Mas já busca os dados pela requisição na internet usando Retrofit. Esse projeto faz parte do livro
Android Apprentice, 4° Edição (https://www.raywenderlich.com/books/android-apprentice). Então, não 
fiz muitas modificações no código. Estou basicamente estudando o código.

- Pacote Adapter
  Está o adapter da RecylerView
  
- Pacote Model
  Estão as classes que representam o podcast e cada episódio da série
  
- Pacote Repository
  Representa uma camada de abstração em relação às classes que realmente
  fazem a requisição web
  
- Pacote Service
  Estão as classes e interfaces que realmente cuidam da requisição web e
  outras classes que representam a resposta quando for transformada em um
  objeto Kotlin
  
- Pacote UI
  Estão as classes de interface por enquanto
  
- Pacote Util
  Está um objeto para fazer formatação de datas
  
- Pacote ViewModel
  Estão as classes que fazem a intermediação dos dados que serão mostrados na UI.
  

Peço que seja testado o código da master, porque vou continuar alterando o código da
outra branch para fazer um posterior merge
