package todomvc

class TodoStorage extends TodoApi {
  private object State {
    var todos = Seq.empty[Todo]

    def mod(f: Seq[Todo] => Seq[Todo]): Seq[Todo] =
      {
        todos = f(todos)
        todos
      }

    def modOne(Id: TodoId)(f: Todo => Todo): Seq[Todo] =
      mod(_.map {
        case existing@Todo(Id, _, _) => f(existing)
        case other                   => other
      })
  }

  override def todos(s: String): Seq[Todo] =
    State.todos

  def addTodo(title: Title): Seq[Todo] =
   State.mod(_ :+ Todo(TodoId.random, title, isCompleted = false))

  def clearCompleted(): Seq[Todo] =
   State.mod(_.filterNot(_.isCompleted))

  def delete(id: TodoId): Seq[Todo] =
   State.mod(_.filterNot(_.id == id))

  def toggleAll(checked: Boolean): Seq[Todo] =
   State.mod(_.map(_.copy(isCompleted = checked)))

  def toggleCompleted(id: TodoId): Seq[Todo] =
   State.modOne(id)(old => old.copy(isCompleted = !old.isCompleted))

  def update(id: TodoId, text: Title): Seq[Todo] =
   State.modOne(id)(_.copy(title = text))
}
