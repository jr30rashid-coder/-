package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.MultipleChoiceQuestion
import com.example.domain.QuizSummary
import com.example.domain.SafetyCheckCard
import com.example.ui.theme.LocalSemanticStatusColors

@Composable
fun SafetyCheckCardsSection(
  cards: List<SafetyCheckCard>,
  answeredCards: Map<String, Boolean>,
  onAnswerCard: (cardId: String, answerIsTrue: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalSemanticStatusColors.current

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = "التمرين 2: بطاقات التحقق من السلامة (صحيح / خطأ)",
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = "قيّم كل عبارة من مبادئ التشغيل المدني الآمن لتظهر لك النتيجة والتفسير الفوري:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    cards.forEachIndexed { index, card ->
      val userAnswer = answeredCards[card.id]
      val isAnswered = userAnswer != null
      val isCorrect = userAnswer == card.expectedIsTrue

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
          width = 1.dp,
          color = when {
            !isAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            isCorrect -> statusColors.success
            else -> statusColors.error
          },
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "بطاقة ${index + 1} • ${card.reviewStatus.labelAr}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Text(
            text = card.statementAr,
            style = MaterialTheme.typography.bodyLarge,
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            val trueSelected = userAnswer == true
            val falseSelected = userAnswer == false

            OutlinedButton(
              onClick = { onAnswerCard(card.id, true) },
              border = BorderStroke(
                width = if (trueSelected) 2.dp else 1.dp,
                color = if (trueSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
              ),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("safety_${card.id}_true"),
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("صحيح")
            }

            OutlinedButton(
              onClick = { onAnswerCard(card.id, false) },
              border = BorderStroke(
                width = if (falseSelected) 2.dp else 1.dp,
                color = if (falseSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
              ),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("safety_${card.id}_false"),
            ) {
              Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("خطأ")
            }
          }

          if (isAnswered) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isCorrect) statusColors.successContainer else statusColors.errorContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(10.dp),
              ) {
                Icon(
                  imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                  contentDescription = if (isCorrect) {
                    stringResource(R.string.cd_correct_badge)
                  } else {
                    stringResource(R.string.cd_incorrect_badge)
                  },
                  tint = if (isCorrect) statusColors.success else statusColors.error,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = if (isCorrect) "إجابة موفّقة" else "تحتاج مراجعة",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCorrect) statusColors.success else statusColors.error,
                  )
                  Text(
                    text = card.explanationAr,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ConceptMcqSection(
  questions: List<MultipleChoiceQuestion>,
  answeredOptions: Map<String, String>,
  onSelectOption: (questionId: String, optionId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalSemanticStatusColors.current

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = "التمرين 3: أسئلة اختيار من متعدد مع التفسير الفوري",
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = "اختر الإجابة الأدق لكل سؤال لقراءة التعليل الهندسي مباشرة:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    questions.forEach { question ->
      val chosenId = answeredOptions[question.id]
      val isAnswered = chosenId != null
      val isCorrect = chosenId == question.correctOptionId

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Text(
            text = question.promptAr,
            style = MaterialTheme.typography.titleSmall,
          )

          question.options.forEach { option ->
            val isSelected = chosenId == option.id
            val isOptionCorrect = option.id == question.correctOptionId
            val borderColor = when {
              !isAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
              isOptionCorrect -> statusColors.success
              isSelected -> statusColors.error
              else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = when {
                isAnswered && isOptionCorrect -> statusColors.successContainer.copy(alpha = 0.7f)
                isAnswered && isSelected -> statusColors.errorContainer.copy(alpha = 0.7f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              },
              border = BorderStroke(if (isSelected || (isAnswered && isOptionCorrect)) 1.5.dp else 1.dp, borderColor),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("mcq_${question.id}_opt_${option.id}")
                .clickable { onSelectOption(question.id, option.id) },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = { onSelectOption(question.id, option.id) },
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = option.textAr,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          if (isAnswered) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isCorrect) statusColors.successContainer else statusColors.errorContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(10.dp),
              ) {
                Icon(
                  imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                  contentDescription = null,
                  tint = if (isCorrect) statusColors.success else statusColors.error,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = if (isCorrect) "إجابة صحيحة" else "تفسير الإجابة الصحيحة",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCorrect) statusColors.success else statusColors.error,
                  )
                  Text(
                    text = question.explanationAr,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SectionReviewQuiz(
  questions: List<MultipleChoiceQuestion>,
  selections: Map<String, String>,
  summary: QuizSummary?,
  onSelectOption: (questionId: String, optionId: String) -> Unit,
  onSubmitQuiz: () -> Unit,
  onRetryQuiz: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalSemanticStatusColors.current

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = "التمرين 4: اختبار نهاية القسم (${selections.size}/${questions.size})",
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = "أجب عن جميع الأسئلة ثم اضغط «اعتماد النتيجة» لعرض ملخص الأداء والمراجعة:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (summary != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quiz_summary_card"),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Text(
            text = "ملخص نتيجة القسم: ${summary.correctCount} من ${summary.totalQuestions} (${summary.percentage}%)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = summary.nonCertificationNoticeAr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedButton(
            onClick = onRetryQuiz,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("retry_review_quiz_button"),
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("إعادة الاختبار")
          }
        }
      }

      summary.results.forEach { result ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(
            width = 1.dp,
            color = if (result.isCorrect) statusColors.success else statusColors.error,
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (result.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (result.isCorrect) statusColors.success else statusColors.error,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (result.isCorrect) "إجابة صحيحة" else "إجابة خاطئة",
                style = MaterialTheme.typography.labelLarge,
                color = if (result.isCorrect) statusColors.success else statusColors.error,
              )
            }
            Text(text = result.question.promptAr, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            Text(text = result.question.explanationAr, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    } else {
      questions.forEach { question ->
        val chosen = selections[question.id]
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
          ) {
            Text(text = question.promptAr, style = MaterialTheme.typography.titleSmall)
            question.options.forEach { option ->
              val isSelected = chosen == option.id
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("quiz_${question.id}_opt_${option.id}")
                  .clickable { onSelectOption(question.id, option.id) },
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                  RadioButton(
                    selected = isSelected,
                    onClick = { onSelectOption(question.id, option.id) },
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = option.textAr,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }
      }

      Button(
        onClick = onSubmitQuiz,
        enabled = selections.size == questions.size,
        modifier = Modifier
          .fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("submit_review_quiz_button"),
      ) {
        Text(
          if (selections.size == questions.size) {
            "اعتماد النتيجة وعرض الملخص"
          } else {
            "أكمل جميع الأسئلة (${selections.size}/${questions.size})"
          },
        )
      }
    }
  }
}
