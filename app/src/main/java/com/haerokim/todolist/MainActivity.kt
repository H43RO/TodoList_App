package com.haerokim.todolist

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haerokim.todolist.databinding.ActivityMainBinding
import com.haerokim.todolist.databinding.ItemTodoBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val data = arrayListOf<Todo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        data.add(Todo("숙제", false))
        data.add(Todo("청소", true))

        binding.recylcerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)

            //to의-do 객체의 주소값이 왔다갔다해서 편하게 뷰를 조작할 수 있음 (객체지향 프로그래밍 특성 활용)
            adapter = TodoAdapter(data,
                onClickDeleteIcon = {
                    deleteTodo(it)
                },
                onClickItem = {
                    toggleTodo(it)
                })
        }

        binding.addButton.setOnClickListener {
            addTodo()
        }

    }

    private fun addTodo() {
        val todo = Todo(binding.editText.text.toString())
        data.add(todo)
        binding.recylcerView.adapter?.notifyDataSetChanged() //null safe call
    }

    private fun deleteTodo(todo: Todo) {
        data.remove(todo)
        binding.recylcerView.adapter?.notifyDataSetChanged() //null safe call
    }

    private fun toggleTodo(todo: Todo) {
        todo.isDone = !todo.isDone
        binding.recylcerView.adapter?.notifyDataSetChanged()
    }
}

data class Todo(
    val text: String,
    var isDone: Boolean = false
) //기본값 false

class TodoAdapter(
    private val myDataset: List<Todo>,
    val onClickDeleteIcon: (todo: Todo) -> Unit,
    val onClickItem: (todo: Todo) -> Unit
) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) //RecyclerView에 붙일 view를 얻음

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TodoAdapter.TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)

        return TodoViewHolder(ItemTodoBinding.bind(view))
    }

    //Item View를 어떻게 보여줄지 결정하는 뷰 홀더
    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = myDataset[position]

        holder.binding.todoText.text = todo.text

        if (todo.isDone) {

            //람다 형식으로 코드를 줄일 수 있다.
            //holder.binding.todoText.paintFlags = holder.binding.todoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.todoText.apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTypeface(null, Typeface.ITALIC)
            }
        } else {
            holder.binding.todoText.apply {
                paintFlags = 0
                setTypeface(null, Typeface.NORMAL)
            }
        }

        holder.binding.deleteImageView.setOnClickListener {
            //Adapter를 사용하고 있는 Activity쪽에 Delete 버튼이 눌림을 알려야함
            onClickDeleteIcon.invoke(todo) //눌렀을 때 to-do 전달
        }

        holder.binding.root.setOnClickListener {
            onClickItem.invoke(todo)
        }
    }

    override fun getItemCount() = myDataset.size
}
