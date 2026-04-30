package app.campfire.ui.theming.ui.ai.emptystate

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun generatePoissonDiskSamples(
  size: Size,
  minDistance: Double,
  k: Int = 30, // Number of samples to choose before rejection (typically 30)
): List<Offset> {
  val cellSize = minDistance / sqrt(2.0)
  val gridWidth = (size.width / cellSize).toInt() + 1
  val gridHeight = (size.height / cellSize).toInt() + 1

  val grid = Array(gridWidth * gridHeight) { -1 } // Stores index of point in cell, or -1 if empty
  val activeList = mutableListOf<Offset>()
  val points = mutableListOf<Offset>()

  // Step 1: Initialize
  // Add a random starting point
  val initialX = Random.nextFloat() * size.width
  val initialY = Random.nextFloat() * size.height
  val initialPoint = Offset(initialX, initialY)

  activeList.add(initialPoint)
  points.add(initialPoint)
  val initialGridX = (initialX / cellSize).toInt()
  val initialGridY = (initialY / cellSize).toInt()
  grid[initialGridY * gridWidth + initialGridX] = points.lastIndex

  // Step 2: Main loop
  while (activeList.isNotEmpty()) {
    val randomIndex = Random.nextInt(activeList.size)
    val currentPoint = activeList[randomIndex]
    var foundCandidate = false

    // Generate k candidate points around the current point
    for (i in 0 until k) {
      val angle = Random.nextFloat() * 2.0 * PI
      val radius = Random.nextDouble(minDistance, 2.0 * minDistance) // Sample between r and 2r

      val candidateX = currentPoint.x + radius * cos(angle)
      val candidateY = currentPoint.y + radius * sin(angle)
      val candidatePoint = Offset(candidateX.toFloat(), candidateY.toFloat())

      // Check if the candidate is valid
      if (candidateX >= 0 && candidateX < size.width &&
        candidateY >= 0 && candidateY < size.height &&
        isCandidateValid(candidatePoint, grid, gridWidth, gridHeight, cellSize, minDistance, points)
      ) {
        points.add(candidatePoint)
        activeList.add(candidatePoint)
        val gridX = (candidateX / cellSize).toInt()
        val gridY = (candidateY / cellSize).toInt()
        grid[gridY * gridWidth + gridX] = points.lastIndex
        foundCandidate = true
      }
    }

    // If no valid candidate was found after k attempts, remove the current point from active list
    if (!foundCandidate) {
      activeList.removeAt(randomIndex)
    }
  }

  return points
}

private fun isCandidateValid(
  candidate: Offset,
  grid: Array<Int>,
  gridWidth: Int,
  gridHeight: Int,
  cellSize: Double,
  minDistance: Double,
  points: List<Offset>,
): Boolean {
  val candidateGridX = (candidate.x / cellSize).toInt()
  val candidateGridY = (candidate.y / cellSize).toInt()

  // Check neighboring cells (including the candidate's cell) in a 5x5 grid
  // (A 3x3 grid around the candidate cell is sufficient if minDistance is the radius for point placement,
  // but a 5x5 grid is safer as points can be up to 2*minDistance away when sampling)
  for (yOffset in -2..2) {
    for (xOffset in -2..2) {
      val neighborGridX = candidateGridX + xOffset
      val neighborGridY = candidateGridY + yOffset

      if (neighborGridX >= 0 && neighborGridX < gridWidth &&
        neighborGridY >= 0 && neighborGridY < gridHeight
      ) {
        val pointIndexInCell = grid[neighborGridY * gridWidth + neighborGridX]
        if (pointIndexInCell != -1) {
          val neighborPoint = points[pointIndexInCell]
          val distSq = distanceSquared(candidate, neighborPoint)
          if (distSq < minDistance * minDistance) {
            return false // Too close to an existing point
          }
        }
      }
    }
  }
  return true
}

private fun distanceSquared(p1: Offset, p2: Offset): Float {
  val dx = p1.x - p2.x
  val dy = p1.y - p2.y
  return dx * dx + dy * dy
}
