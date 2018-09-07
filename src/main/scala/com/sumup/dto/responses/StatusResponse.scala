package com.sumup.dto.responses

trait StatusResponse {
  def code: Int
  def `type`: String
  def message: String
}
