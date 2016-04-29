package io.glassdoor.application


object UniqueIdGenerator {
  private var nextId:Long  = 1

  def generate():Long = {
    var result = nextId

    if(nextId >= Long.MaxValue){
      nextId = 0
    }

    nextId += 1
    result
  }
}
