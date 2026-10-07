import models.Categories
import models.TaskHistory
import models.Tasks
import models.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object DatabaseFactory {

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun init() {
        // SQLite-файл создаётся в рабочей директории сервера
        Database.connect(
            url = "jdbc:sqlite:taskplanner.db",
            driver = "org.sqlite.JDBC"
        )

        transaction {
            SchemaUtils.create(Users, Categories, Tasks, TaskHistory)
            if (Users.selectAll().empty()) {
                seed()
            }
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    fun now(): String = LocalDateTime.now().format(DATE_TIME)

    private fun seed() {
        val today = LocalDate.now()

        // --- Пользователи ---
        val adminId = newId()
        val userId = newId()
        Users.insert {
            it[id] = adminId
            it[name] = "Администратор"
            it[login] = "admin"
            it[email] = "admin@taskplanner.app"
            it[passwordHash] = BCrypt.hashpw("admin123", BCrypt.gensalt())
            it[role] = "ADMIN"
        }
        Users.insert {
            it[id] = userId
            it[name] = "Иван Пользователь"
            it[login] = "user"
            it[email] = "user@taskplanner.app"
            it[passwordHash] = BCrypt.hashpw("user123", BCrypt.gensalt())
            it[role] = "USER"
        }

        // --- Категории ---
        val categoryNames = listOf("Работа", "Учёба", "Дом", "Здоровье", "Финансы", "Личное")
        val categoryIds = HashMap<String, String>()
        categoryNames.forEach { categoryName ->
            val cid = newId()
            categoryIds[categoryName] = cid
            Categories.insert {
                it[id] = cid
                it[name] = categoryName
            }
        }

        // helper для создания задачи; возвращает id задачи
        fun createTask(
            owner: String,
            title: String,
            description: String,
            category: String,
            priority: String,
            status: String,
            dueInDays: Long
        ): String {
            val tid = newId()
            val moment = now()
            Tasks.insert {
                it[id] = tid
                it[Tasks.userId] = owner
                it[categoryId] = categoryIds.getValue(category)
                it[Tasks.title] = title
                it[Tasks.description] = description
                it[Tasks.priority] = priority
                it[Tasks.status] = status
                it[dueDate] = today.plusDays(dueInDays).format(DATE)
                it[createdAt] = moment
                it[updatedAt] = moment
            }
            return tid
        }

        fun addHistory(taskId: String, from: String, to: String, who: String) {
            TaskHistory.insert {
                it[id] = newId()
                it[TaskHistory.taskId] = taskId
                it[oldStatus] = from
                it[newStatus] = to
                it[changedAt] = now()
                it[changedBy] = who
            }
        }

        // --- Задачи обычного пользователя ---
        val t1 = createTask(
            userId, "Подготовить отчёт по практике",
            "Собрать материалы, оформить титульный лист и распечатать отчёт для сдачи руководителю.",
            "Учёба", "HIGH", "IN_PROGRESS", 3
        )
        addHistory(t1, "NEW", "IN_PROGRESS", "Иван Пользователь")

        createTask(
            userId, "Купить продукты на неделю",
            "Молоко, хлеб, овощи, крупы. Заехать в магазин после работы.",
            "Дом", "LOW", "NEW", 1
        )

        val t3 = createTask(
            userId, "Оплатить коммунальные услуги",
            "Передать показания счётчиков и оплатить квитанции через мобильное приложение банка.",
            "Финансы", "MEDIUM", "DONE", -2
        )
        addHistory(t3, "NEW", "IN_PROGRESS", "Иван Пользователь")
        addHistory(t3, "IN_PROGRESS", "DONE", "Иван Пользователь")

        createTask(
            userId, "Записаться к стоматологу",
            "Позвонить в клинику и записаться на профилактический осмотр.",
            "Здоровье", "MEDIUM", "NEW", -1
        )

        createTask(
            userId, "Сдать лабораторную работу №4",
            "Доделать программу по дисциплине «Разработка клиент-серверных мобильных приложений» и защитить работу.",
            "Учёба", "HIGH", "NEW", 5
        )

        val t6 = createTask(
            userId, "Подготовить презентацию для планёрки",
            "Сделать 10 слайдов с итогами спринта и показателями команды.",
            "Работа", "HIGH", "IN_PROGRESS", 2
        )
        addHistory(t6, "NEW", "IN_PROGRESS", "Иван Пользователь")

        createTask(
            userId, "Изучить документацию Jetpack Compose",
            "Разобрать разделы про состояние, навигацию и Material 3 перед выполнением курсовой работы.",
            "Учёба", "MEDIUM", "IN_PROGRESS", 6
        )

        createTask(
            userId, "Позвонить родителям",
            "Узнать, как дела, и договориться о поездке на выходных.",
            "Личное", "LOW", "NEW", 0
        )

        val t8 = createTask(
            userId, "Разобрать рабочую почту",
            "Ответить на письма клиентов и разложить входящие по папкам.",
            "Работа", "MEDIUM", "DONE", -3
        )
        addHistory(t8, "NEW", "DONE", "Иван Пользователь")

        createTask(
            userId, "Сделать генеральную уборку",
            "Помыть окна, разобрать шкаф, вынести ненужные вещи.",
            "Дом", "LOW", "NEW", 7
        )

        createTask(
            userId, "Записаться в спортивный зал",
            "Выбрать абонемент и оформить его на три месяца.",
            "Здоровье", "LOW", "NEW", 10
        )

        // --- Задачи администратора ---
        val t11 = createTask(
            adminId, "Проверить резервные копии базы данных",
            "Убедиться, что ежедневная резервная копия создаётся и восстанавливается без ошибок.",
            "Работа", "HIGH", "IN_PROGRESS", 1
        )
        addHistory(t11, "NEW", "IN_PROGRESS", "Администратор")

        createTask(
            adminId, "Обновить справочник категорий",
            "Проанализировать используемые категории задач и при необходимости добавить новые.",
            "Работа", "MEDIUM", "NEW", 4
        )

        createTask(
            adminId, "Составить бюджет на квартал",
            "Свести расходы и запланировать бюджет на следующий квартал.",
            "Финансы", "MEDIUM", "NEW", 12
        )
    }
}
