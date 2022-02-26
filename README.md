# Este é o PodPlay

Um app para ouvir podcasts. Nele é possível ouvir podcasts e inscrever-se nos podcasts que você mais gosta. Quando você se inscreve em um podcast,
o app regularmente verifica se há novos episódios. Em caso afirmativo, o usuário é notificado. Todos os podcasts são buscados na API do iTunes.

Este projeto faz parte do livro Android Apprentice, 4° Edição (https://www.raywenderlich.com/books/android-apprentice). A base do código está neste livro,
com algumas alterações feitas por mim quando encontrava algum ou quando queria que o código ficasse mais fácil de entender.

Existem mais pacotes que os listados abaixo. Em breve farei a atualização.

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
  

