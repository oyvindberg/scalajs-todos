package todomvc

trait TodoApi {
  def todos(s: String): Seq[Todo]
  def addTodo(title: Title): Seq[Todo]
  def clearCompleted(): Seq[Todo]
  def delete(id: TodoId): Seq[Todo]
  def toggleAll(checked: Boolean): Seq[Todo]
  def toggleCompleted(id: TodoId): Seq[Todo]
  def update(id: TodoId, text: Title): Seq[Todo]
}
