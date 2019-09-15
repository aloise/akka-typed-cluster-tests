package name.aloise.actors

import java.util.UUID

final case class DeviceId(id: UUID) extends AnyVal
object DeviceId {
  def random: DeviceId = DeviceId(UUID.randomUUID())
}
