import { useEffect, useState } from 'react'

/** Milisegundos que faltan hasta `hasta` (nunca negativo), recalculados cada segundo. */
export function useMsRestantes(hasta: string | null | undefined): number {
  const [ahora, setAhora] = useState(() => Date.now())

  useEffect(() => {
    const timer = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  if (!hasta) return 0
  return Math.max(0, new Date(hasta).getTime() - ahora)
}

/** "1:45" por debajo de una hora, "5 h 12 min" por debajo de un día, "2 d 3 h" arriba. */
export function formatRestante(ms: number): string {
  const totalSegundos = Math.ceil(ms / 1000)
  const dias = Math.floor(totalSegundos / 86_400)
  const horas = Math.floor((totalSegundos % 86_400) / 3600)
  const minutos = Math.floor((totalSegundos % 3600) / 60)
  const segundos = totalSegundos % 60

  if (dias > 0) return `${dias} d ${horas} h`
  if (horas > 0) return `${horas} h ${minutos} min`
  return `${minutos}:${String(segundos).padStart(2, '0')}`
}
